const FMT = {
    PAPER:     { label: "Księga papierowa", days: 14 },
    EBOOK:     { label: "E-book",           days: 30 },
    AUDIOBOOK: { label: "Audiobook",        days: 21 },
};

const state = {
    token: localStorage.getItem("lor_token") || null,
    user:  JSON.parse(localStorage.getItem("lor_user")  || "null"),
    view:  "catalog",
    strategy: "title",
};

const $  = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, c => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const isAdmin = () => state.user?.role === "ADMIN";

function resetNode(el) {
    const fresh = el.cloneNode(false);
    el.replaceWith(fresh);
    return fresh;
}

async function api(path, { method = "GET", body, auth = true } = {}) {
    const headers = {};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (auth && state.token) headers["Authorization"] = "Bearer " + state.token;

    const res = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });

    if (res.status === 401) { logout(true); throw new Error("Twoja pieczęć wygasła. Wejdź ponownie."); }

    const ctype = res.headers.get("content-type") || "";
    const payload = ctype.includes("application/json")
        ? await res.json().catch(() => null)
        : await res.text();

    if (!res.ok) {
        const msg = (payload && typeof payload === "object" && (payload.message || payload.error))
            || (typeof payload === "string" && payload) || `Błąd ${res.status}`;
        if (res.status === 403) throw new Error("Ten krąg jest dostępny tylko dla Archiwistów (ADMIN).");
        throw new Error(msg);
    }
    return payload;
}

function toast(msg, kind = "") {
    const host = $("#toasts");
    const el = document.createElement("div");
    el.className = "toast" + (kind ? ` toast--${kind}` : "");
    el.innerHTML = `<b>${kind === "err" ? "✖ " : kind === "ok" ? "✦ " : ""}</b>${esc(msg)}`;
    host.appendChild(el);
    setTimeout(() => { el.classList.add("toast--out"); setTimeout(() => el.remove(), 350); }, 4200);
}

function setSession(data) {
    state.token = data.token;
    state.user  = { id: data.id, username: data.username, role: data.role };
    localStorage.setItem("lor_token", state.token);
    localStorage.setItem("lor_user", JSON.stringify(state.user));
}

function logout(silent = false) {
    state.token = null; state.user = null;
    localStorage.removeItem("lor_token"); localStorage.removeItem("lor_user");
    $("#app").classList.add("hidden");
    $("#gate").classList.remove("hidden");
    if (!silent) toast("Opuściłeś bibliotekę.", "");
}

function enterApp() {
    $("#gate").classList.add("hidden");
    $("#app").classList.remove("hidden");
    $("#userBadge").innerHTML =
        `<b>${esc(state.user.username)}</b> · <span class="role">${esc(state.user.role)}</span>`;
    $$(".navlink--admin").forEach(b => b.classList.toggle("hidden", !isAdmin()));
    navigate(state.view || "catalog");
}

$("#authTabs").addEventListener("click", e => {
    const which = e.target.dataset.auth;
    if (!which) return;
    $$(".tab").forEach(t => t.classList.toggle("tab--active", t.dataset.auth === which));
    $("#loginForm").classList.toggle("hidden", which !== "login");
    $("#registerForm").classList.toggle("hidden", which !== "register");
});

$("#loginForm").addEventListener("submit", async e => {
    e.preventDefault();
    const f = new FormData(e.target);
    try {
        const data = await api("/api/auth/login", { auth: false, method: "POST",
            body: { username: f.get("username"), password: f.get("password") } });
        setSession(data);
        toast(`Witaj w bibliotece, ${data.username}.`, "ok");
        enterApp();
    } catch (err) { toast(err.message, "err"); }
});

$("#registerForm").addEventListener("submit", async e => {
    e.preventDefault();
    const f = new FormData(e.target);
    try {
        await api("/api/auth/register", { auth: false, method: "POST",
            body: { username: f.get("username"), email: f.get("email"), password: f.get("password") } });
        toast("Przysięga złożona. Możesz teraz wejść.", "ok");
        // auto-login
        const data = await api("/api/auth/login", { auth: false, method: "POST",
            body: { username: f.get("username"), password: f.get("password") } });
        setSession(data); enterApp();
    } catch (err) { toast(err.message, "err"); }
});

$("#logoutBtn").addEventListener("click", () => logout());

$("#nav").addEventListener("click", e => {
    const v = e.target.dataset.view;
    if (v) navigate(v);
});

function navigate(view) {
    state.view = view;
    $$(".navlink").forEach(b => b.classList.toggle("navlink--active", b.dataset.view === view));
    const view$ = resetNode($("#view"));  // świeży kontener - zrzuca stare listenery
    view$.innerHTML = `<div class="loading">Odczytywanie indeksu…</div>`;
    ({ catalog: viewCatalog, search: viewSearch, loans: viewLoans,
       reservations: viewReservations, admin: viewAdmin }[view] || viewCatalog)(view$);
}

function authorsOf(b) {
    if (!b.authors || !b.authors.length) return "Autor nieznany";
    return b.authors.map(a => `${a.firstName} ${a.lastName}`).join(", ");
}

function tomeCard(b) {
    const out = (b.availableCopies ?? 0) <= 0;
    const fmt = FMT[b.format]?.label || b.format;
    return `
    <article class="tome" data-book="${b.id}">
        ${b.category ? `<span class="tome__cat">${esc(b.category.name)}</span>` : ""}
        <span class="tome__fmt">${esc(fmt)}</span>
        <h3 class="tome__title">${esc(b.title)}</h3>
        <p class="tome__authors">${esc(authorsOf(b))}</p>
        <div class="tome__meta">
            <span class="tome__isbn">ISBN ${esc(b.isbn)}</span>
            ${b.publicationYear ? `<span>${esc(b.publicationYear)}</span>` : ""}
        </div>
        <div class="tome__avail ${out ? "out" : ""}">Dostępne: <b>${b.availableCopies ?? 0}</b> / ${b.totalCopies ?? 0}</div>
        <div class="tome__actions">
            <button class="btn btn--ghost btn--sm" data-act="detail" data-id="${b.id}">Wejrzyj</button>
            <button class="btn btn--seal btn--sm" data-act="borrow" data-id="${b.id}" ${out ? "disabled" : ""}>Wypożycz</button>
            <button class="btn btn--gold btn--sm" data-act="reserve" data-id="${b.id}" ${out ? "disabled" : ""}>Rezerwuj</button>
        </div>
    </article>`;
}

function bindTomeActions(root) {
    root.addEventListener("click", async e => {
        const btn = e.target.closest("[data-act]");
        if (!btn || btn.disabled) return;
        const id = +btn.dataset.id;
        if (btn.dataset.act === "detail") return openBookModal(id);
        // Blokujemy przycisk na czas żądania - zapobiega podwójnym wypożyczeniom
        btn.disabled = true;
        try {
            if (btn.dataset.act === "borrow")  await doBorrow(id);
            if (btn.dataset.act === "reserve") await doReserve(id);
        } finally {
            if (document.body.contains(btn)) btn.disabled = false;
        }
    });
}

async function doBorrow(bookId) {
    try {
        await api("/api/loans", { method: "POST", body: { userId: state.user.id, bookId } });
        toast("Księga wypożyczona. Pamiętaj o terminie zwrotu.", "ok");
        navigate(state.view);
    } catch (err) { toast(err.message, "err"); }
}
async function doReserve(bookId) {
    try {
        await api("/api/reservations", { method: "POST", body: { userId: state.user.id, bookId } });
        toast("Rezerwacja zapisana w rejestrze.", "ok");
    } catch (err) { toast(err.message, "err"); }
}

async function viewCatalog(root) {
    try {
        const books = await api("/api/books");
        root.innerHTML = `
            <div class="vhead">
                <p class="vhead__kick">Indeks · ${books.length} woluminów</p>
                <h2 class="vhead__title">Katalog Główny</h2>
                <p class="vhead__sub">Każdy tom skatalogowany, każdy egzemplarz policzony. Wybierz, a zostanie zapisane.</p>
            </div>
            ${books.length ? `<div class="grid">${books.map(tomeCard).join("")}</div>`
                           : `<div class="empty">Indeks jest pusty. Archiwiści jeszcze nie złożyli ksiąg.</div>`}`;
        bindTomeActions(root);
    } catch (err) { root.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
}

const STRATS = [
    { key: "title",     label: "Po tytule" },
    { key: "author",    label: "Po autorze" },
    { key: "category",  label: "Po kategorii" },
    { key: "available", label: "Tylko dostępne" },
];

function viewSearch(root) {
    root.innerHTML = `
        <div class="vhead">
            <p class="vhead__kick">Wyrocznia · wzorzec Strategy</p>
            <h2 class="vhead__title">Wyszukiwanie</h2>
            <p class="vhead__sub">Cztery algorytmy, jeden kontrakt. Wybierz strategię i zadaj pytanie indeksowi.</p>
        </div>
        <div class="strategy-pills" id="strats">
            ${STRATS.map(s => `<button class="pill ${s.key === state.strategy ? "pill--on" : ""}" data-strat="${s.key}">${s.label}</button>`).join("")}
        </div>
        <div class="oracle">
            <label class="field">
                <span>Fraza</span>
                <input id="searchValue" placeholder="czego szukasz w mroku…">
            </label>
            <button class="btn btn--gold" id="searchBtn">Pytaj</button>
        </div>
        <div id="searchResults"></div>`;

    const valInput = $("#searchValue", root);
    const updatePills = () => $$(".pill", root).forEach(p =>
        p.classList.toggle("pill--on", p.dataset.strat === state.strategy));
    const toggleVal = () => valInput.parentElement.style.display =
        state.strategy === "available" ? "none" : "flex";
    toggleVal();

    $("#strats", root).addEventListener("click", e => {
        const s = e.target.dataset.strat; if (!s) return;
        state.strategy = s; updatePills(); toggleVal();
    });
    $("#searchBtn", root).addEventListener("click", runSearch);
    valInput.addEventListener("keydown", e => { if (e.key === "Enter") runSearch(); });

    async function runSearch() {
        const out = resetNode($("#searchResults", root));  // świeży kontener wyników
        out.innerHTML = `<div class="loading">Wyrocznia rozważa…</div>`;
        try {
            const q = new URLSearchParams({ strategy: state.strategy, value: valInput.value || "" });
            const books = await api("/api/books/search?" + q.toString());
            out.innerHTML = books.length
                ? `<div class="grid">${books.map(tomeCard).join("")}</div>`
                : `<div class="empty">Wyrocznia milczy. Nic nie pasuje do tej frazy.</div>`;
            bindTomeActions(out);
        } catch (err) { out.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
    }
}

async function viewLoans(root) {
    try {
        const loans = await api(`/api/loans/user/${state.user.id}`);
        root.innerHTML = `
            <div class="vhead">
                <p class="vhead__kick">Rejestr Długów · ${loans.length}</p>
                <h2 class="vhead__title">Moje Wypożyczenia</h2>
                <p class="vhead__sub">Historia tego, co zabrałeś z biblioteki. Termin zwrotu liczony jest z natury księgi.</p>
            </div>
            ${loans.length ? loanTable(loans, true) : `<div class="empty">Nie masz żadnych długów wobec biblioteki.</div>`}`;
        bindLoanActions(root);
    } catch (err) { root.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
}

function loanTable(loans, mine) {
    return `<table class="ledger">
        <thead><tr>
            <th>#</th><th>Księga</th>${mine ? "" : "<th>Czytelnik</th>"}
            <th>Wypożyczono</th><th>Termin</th><th>Status</th><th></th>
        </tr></thead>
        <tbody>${loans.map(l => `
            <tr>
                <td class="num">${l.id}</td>
                <td>${esc(l.book?.title || "—")}</td>
                ${mine ? "" : `<td>${esc(l.user?.username || "—")}</td>`}
                <td class="num">${fmtDate(l.borrowedAt)}</td>
                <td class="num">${fmtDate(l.dueDate)}</td>
                <td><span class="chip chip--${l.status}">${l.status}</span></td>
                <td>${l.status === "BORROWED"
                    ? `<button class="btn btn--gold btn--sm" data-return="${l.id}">Zwróć</button>` : ""}</td>
            </tr>`).join("")}</tbody></table>`;
}

function bindLoanActions(root) {
    root.addEventListener("click", async e => {
        const id = e.target.dataset.return; if (!id) return;
        try {
            await api(`/api/loans/${id}/return`, { method: "PATCH" });
            toast("Księga wróciła na półkę. Dług spłacony.", "ok");
            navigate(state.view);
        } catch (err) { toast(err.message, "err"); }
    });
}

async function viewReservations(root) {
    try {
        const rs = await api(`/api/reservations/user/${state.user.id}`);
        root.innerHTML = `
            <div class="vhead">
                <p class="vhead__kick">Rejestr Rezerw · ${rs.length}</p>
                <h2 class="vhead__title">Moje Rezerwacje</h2>
                <p class="vhead__sub">Księgi odłożone na bok, czekające aż po nie sięgniesz.</p>
            </div>
            ${rs.length ? reservationTable(rs, true) : `<div class="empty">Brak rezerwacji w rejestrze.</div>`}`;
        bindReservationActions(root);
    } catch (err) { root.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
}

function reservationTable(rs, mine) {
    return `<table class="ledger">
        <thead><tr>
            <th>#</th><th>Księga</th>${mine ? "" : "<th>Czytelnik</th>"}
            <th>Zarezerwowano</th><th>Status</th><th></th>
        </tr></thead>
        <tbody>${rs.map(r => `
            <tr>
                <td class="num">${r.id}</td>
                <td>${esc(r.book?.title || "—")}</td>
                ${mine ? "" : `<td>${esc(r.user?.username || "—")}</td>`}
                <td class="num">${fmtDate(r.reservedAt)}</td>
                <td><span class="chip chip--${r.status}">${r.status}</span></td>
                <td>${(r.status === "PENDING" || r.status === "CONFIRMED")
                    ? `<button class="btn btn--danger btn--sm" data-cancel="${r.id}">Anuluj</button>` : ""}</td>
            </tr>`).join("")}</tbody></table>`;
}

function bindReservationActions(root) {
    root.addEventListener("click", async e => {
        const id = e.target.dataset.cancel; if (!id) return;
        try {
            await api(`/api/reservations/${id}/cancel`, { method: "PATCH" });
            toast("Rezerwacja anulowana.", "ok");
            navigate(state.view);
        } catch (err) { toast(err.message, "err"); }
    });
}

async function viewAdmin(root) {
    if (!isAdmin()) { root.innerHTML = `<div class="empty">Tylko Archiwiści mają wstęp do Skryptorium.</div>`; return; }
    root.innerHTML = `
        <div class="vhead">
            <p class="vhead__kick">Skryptorium · krąg Archiwistów</p>
            <h2 class="vhead__title">Zarządzanie</h2>
        </div>
        <div class="subtabs" id="adminTabs">
            <button class="subtab subtab--on" data-tab="books">Księgi</button>
            <button class="subtab" data-tab="authors">Autorzy</button>
            <button class="subtab" data-tab="categories">Kategorie</button>
            <button class="subtab" data-tab="ledgers">Rejestry</button>
            <button class="subtab" data-tab="users">Czytelnicy</button>
        </div>
        <div id="adminBody"></div>`;
    const body = $("#adminBody", root);
    $("#adminTabs", root).addEventListener("click", e => {
        const t = e.target.dataset.tab; if (!t) return;
        $$(".subtab", root).forEach(s => s.classList.toggle("subtab--on", s.dataset.tab === t));
        ({ books: adminBooks, authors: adminAuthors, categories: adminCategories,
           ledgers: adminLedgers, users: adminUsers }[t])(body);
    });
    adminBooks(body);
}

async function adminBooks(body) {
    body = resetNode(document.getElementById("adminBody"));  // świeży kontener
    body.innerHTML = `<div class="loading">Wczytywanie…</div>`;
    const [books, cats, authors] = await Promise.all([
        api("/api/books"), api("/api/categories"), api("/api/authors") ]);

    const catOpts = cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join("");
    const authOpts = authors.map(a => `<option value="${a.id}">${esc(a.firstName)} ${esc(a.lastName)}</option>`).join("");

    body.innerHTML = `
        <div class="panel">
            <h3 class="panel__title">Nowa Księga · Factory Method</h3>
            <form id="bookForm">
                <div class="formgrid">
                    <label class="field col-2"><span>Tytuł</span><input name="title" required></label>
                    <label class="field"><span>ISBN</span><input name="isbn" required></label>
                    <label class="field"><span>Rok wydania</span><input name="publicationYear" type="number"></label>
                    <label class="field"><span>Format</span>
                        <select name="format" id="fmtSel" required>
                            <option value="PAPER">Księga papierowa (14 dni)</option>
                            <option value="EBOOK">E-book (30 dni)</option>
                            <option value="AUDIOBOOK">Audiobook (21 dni)</option>
                        </select></label>
                    <label class="field"><span>Liczba egzemplarzy</span><input name="totalCopies" type="number" min="1" value="1" required></label>
                    <label class="field"><span>Kategoria</span><select name="categoryId"><option value="">— brak —</option>${catOpts}</select></label>
                    <label class="field col-2"><span>Autorzy (Ctrl+klik = wielu)</span><select name="authorIds" multiple size="3">${authOpts}</select></label>
                    <label class="field" data-fmt="PAPER"><span>Strony</span><input name="pages" type="number"></label>
                    <label class="field hidden" data-fmt="EBOOK"><span>Format pliku</span><input name="fileFormat" placeholder="PDF / EPUB / MOBI"></label>
                    <label class="field hidden" data-fmt="AUDIOBOOK"><span>Długość (min)</span><input name="durationMinutes" type="number"></label>
                    <label class="field hidden" data-fmt="AUDIOBOOK"><span>Lektor</span><input name="narrator"></label>
                    <label class="field col-2"><span>Opis</span><textarea name="description" rows="2"></textarea></label>
                </div>
                <div class="formrow"><button class="btn btn--seal" type="submit">Złóż Księgę do Indeksu</button></div>
            </form>
        </div>
        <div class="panel">
            <h3 class="panel__title">Indeks · ${books.length}</h3>
            <table class="ledger"><thead><tr><th>#</th><th>Tytuł</th><th>Format</th><th>ISBN</th><th>Dostępne</th><th></th></tr></thead>
            <tbody>${books.map(b => `<tr>
                <td class="num">${b.id}</td><td>${esc(b.title)}</td>
                <td><span class="tome__fmt" style="display:inline-block">${esc(b.format)}</span></td>
                <td class="num">${esc(b.isbn)}</td>
                <td class="num">${b.availableCopies}/${b.totalCopies}</td>
                <td><button class="btn btn--danger btn--sm" data-delbook="${b.id}">Usuń</button></td>
            </tr>`).join("")}</tbody></table>
        </div>`;

    const fmtSel = $("#fmtSel", body);
    const syncFmt = () => $$("[data-fmt]", body).forEach(el =>
        el.classList.toggle("hidden", el.dataset.fmt !== fmtSel.value));
    fmtSel.addEventListener("change", syncFmt); syncFmt();

    $("#bookForm", body).addEventListener("submit", async e => {
        e.preventDefault();
        const f = new FormData(e.target);
        const num = v => v === "" || v == null ? null : Number(v);
        const payload = {
            title: f.get("title"), isbn: f.get("isbn"),
            publicationYear: num(f.get("publicationYear")),
            format: f.get("format"), totalCopies: num(f.get("totalCopies")),
            description: f.get("description") || null,
            categoryId: f.get("categoryId") ? num(f.get("categoryId")) : null,
            authorIds: $$("select[name=authorIds] option:checked", e.target).map(o => +o.value),
            pages: num(f.get("pages")), fileFormat: f.get("fileFormat") || null,
            durationMinutes: num(f.get("durationMinutes")), narrator: f.get("narrator") || null,
        };
        try {
            await api("/api/books", { method: "POST", body: payload });
            toast(`„${payload.title}” dołączyła do Indeksu.`, "ok");
            adminBooks(body);
        } catch (err) { toast(err.message, "err"); }
    });

    body.addEventListener("click", async e => {
        const id = e.target.dataset.delbook; if (!id) return;
        if (!confirm("Usunąć księgę z Indeksu na zawsze?")) return;
        try { await api(`/api/books/${id}`, { method: "DELETE" }); toast("Księga wymazana.", "ok"); adminBooks(body); }
        catch (err) { toast(err.message, "err"); }
    });
}

async function adminAuthors(body) {
    body = resetNode(document.getElementById("adminBody"));
    body.innerHTML = `<div class="loading">Wczytywanie…</div>`;
    const authors = await api("/api/authors");
    body.innerHTML = `
        <div class="panel">
            <h3 class="panel__title">Nowy Autor</h3>
            <form id="authorForm"><div class="formgrid">
                <label class="field"><span>Imię</span><input name="firstName" required></label>
                <label class="field"><span>Nazwisko</span><input name="lastName" required></label>
                <label class="field col-2"><span>Biografia</span><textarea name="bio" rows="2"></textarea></label>
            </div><div class="formrow"><button class="btn btn--seal" type="submit">Zapisz Autora</button></div></form>
        </div>
        <div class="panel">
            <h3 class="panel__title">Autorzy · ${authors.length}</h3>
            <table class="ledger"><thead><tr><th>#</th><th>Imię i nazwisko</th><th>Biografia</th><th></th></tr></thead>
            <tbody>${authors.map(a => `<tr><td class="num">${a.id}</td>
                <td>${esc(a.firstName)} ${esc(a.lastName)}</td>
                <td style="color:var(--bone-dim)">${esc((a.bio||"").slice(0,80)) || "—"}</td>
                <td><button class="btn btn--danger btn--sm" data-delauthor="${a.id}">Usuń</button></td></tr>`).join("")}</tbody></table>
        </div>`;
    $("#authorForm", body).addEventListener("submit", async e => {
        e.preventDefault(); const f = new FormData(e.target);
        try { await api("/api/authors", { method: "POST",
            body: { firstName: f.get("firstName"), lastName: f.get("lastName"), bio: f.get("bio") || null } });
            toast("Autor zapisany.", "ok"); adminAuthors(body);
        } catch (err) { toast(err.message, "err"); }
    });
    body.addEventListener("click", async e => {
        const id = e.target.dataset.delauthor; if (!id) return;
        try { await api(`/api/authors/${id}`, { method: "DELETE" }); toast("Autor usunięty.", "ok"); adminAuthors(body); }
        catch (err) { toast(err.message, "err"); }
    });
}

async function adminCategories(body) {
    body = resetNode(document.getElementById("adminBody"));
    body.innerHTML = `<div class="loading">Wczytywanie…</div>`;
    const cats = await api("/api/categories");
    body.innerHTML = `
        <div class="panel">
            <h3 class="panel__title">Nowa Kategoria</h3>
            <form id="catForm"><div class="formgrid">
                <label class="field col-2"><span>Nazwa</span><input name="name" required></label>
            </div><div class="formrow"><button class="btn btn--seal" type="submit">Zapisz Kategorię</button></div></form>
        </div>
        <div class="panel">
            <h3 class="panel__title">Kategorie · ${cats.length}</h3>
            <table class="ledger"><thead><tr><th>#</th><th>Nazwa</th><th></th></tr></thead>
            <tbody>${cats.map(c => `<tr><td class="num">${c.id}</td><td>${esc(c.name)}</td>
                <td><button class="btn btn--danger btn--sm" data-delcat="${c.id}">Usuń</button></td></tr>`).join("")}</tbody></table>
        </div>`;
    $("#catForm", body).addEventListener("submit", async e => {
        e.preventDefault(); const f = new FormData(e.target);
        try { await api("/api/categories", { method: "POST", body: { name: f.get("name") } });
            toast("Kategoria zapisana.", "ok"); adminCategories(body);
        } catch (err) { toast(err.message, "err"); }
    });
    body.addEventListener("click", async e => {
        const id = e.target.dataset.delcat; if (!id) return;
        try { await api(`/api/categories/${id}`, { method: "DELETE" }); toast("Kategoria usunięta.", "ok"); adminCategories(body); }
        catch (err) { toast(err.message, "err"); }
    });
}

async function adminLedgers(body) {
    body = resetNode(document.getElementById("adminBody"));
    body.innerHTML = `<div class="loading">Wczytywanie rejestrów…</div>`;
    const [loans, rs] = await Promise.all([ api("/api/loans"), api("/api/reservations") ]);
    body.innerHTML = `
        <div class="panel"><h3 class="panel__title">Wszystkie Wypożyczenia · ${loans.length}</h3>
            ${loans.length ? loanTable(loans, false) : `<div class="empty">Brak wypożyczeń.</div>`}</div>
        <div class="panel"><h3 class="panel__title">Wszystkie Rezerwacje · ${rs.length}</h3>
            ${rs.length ? reservationTable(rs, false) : `<div class="empty">Brak rezerwacji.</div>`}</div>`;
    bindLoanActions(body); bindReservationActions(body);
}

async function adminUsers(body) {
    body = resetNode(document.getElementById("adminBody"));
    body.innerHTML = `<div class="loading">Wczytywanie…</div>`;
    const users = await api("/api/users");
    body.innerHTML = `
        <div class="panel"><h3 class="panel__title">Czytelnicy · ${users.length}</h3>
            <table class="ledger"><thead><tr><th>#</th><th>Imię</th><th>E-mail</th><th>Rola</th><th></th></tr></thead>
            <tbody>${users.map(u => `<tr>
                <td class="num">${u.id}</td><td>${esc(u.username)}</td>
                <td style="color:var(--bone-dim)">${esc(u.email)}</td>
                <td><span class="chip chip--${u.role}">${u.role}</span></td>
                <td>
                    ${u.role !== "ADMIN" ? `<button class="btn btn--gold btn--sm" data-promote="${u.id}">Wynieś na Archiwistę</button>` : ""}
                    ${u.id !== state.user.id ? `<button class="btn btn--danger btn--sm" data-deluser="${u.id}">Usuń</button>` : ""}
                </td></tr>`).join("")}</tbody></table></div>`;
    body.addEventListener("click", async e => {
        const promote = e.target.dataset.promote, del = e.target.dataset.deluser;
        try {
            if (promote) { await api(`/api/users/${promote}/promote`, { method: "PUT" }); toast("Czytelnik wyniesiony na Archiwistę.", "ok"); adminUsers(body); }
            if (del) { if (!confirm("Usunąć czytelnika?")) return; await api(`/api/users/${del}`, { method: "DELETE" }); toast("Czytelnik usunięty.", "ok"); adminUsers(body); }
        } catch (err) { toast(err.message, "err"); }
    });
}

async function openBookModal(id) {
    const host = $("#modalHost");
    host.classList.remove("hidden");
    host.innerHTML = `<div class="modal"><div class="loading">Otwieranie tomu…</div></div>`;
    try {
        const [b, formatInfo] = await Promise.all([
            api(`/api/books/${id}`),
            api(`/api/books/${id}/format-info`).catch(() => null),
        ]);
        const out = (b.availableCopies ?? 0) <= 0;
        host.innerHTML = `
        <div class="modal">
            <button class="modal__close" aria-label="Zamknij">×</button>
            <p class="modal__fmt">${esc(FMT[b.format]?.label || b.format)} · termin ${FMT[b.format]?.days ?? "?"} dni</p>
            <h2 class="modal__title">${esc(b.title)}</h2>
            <p class="modal__authors">${esc(authorsOf(b))}</p>
            ${formatInfo ? `<p class="modal__formatinfo">${esc(formatInfo)}</p>` : ""}
            ${b.description ? `<p class="modal__desc">${esc(b.description)}</p>` : ""}
            <dl class="dl">
                <dt>ISBN</dt><dd>${esc(b.isbn)}</dd>
                <dt>Kategoria</dt><dd>${esc(b.category?.name || "—")}</dd>
                ${b.publicationYear ? `<dt>Rok</dt><dd>${esc(b.publicationYear)}</dd>` : ""}
                ${b.pages ? `<dt>Strony</dt><dd>${esc(b.pages)}</dd>` : ""}
                ${b.fileFormat ? `<dt>Plik</dt><dd>${esc(b.fileFormat)}</dd>` : ""}
                ${b.durationMinutes ? `<dt>Długość</dt><dd>${esc(b.durationMinutes)} min</dd>` : ""}
                ${b.narrator ? `<dt>Lektor</dt><dd>${esc(b.narrator)}</dd>` : ""}
                <dt>Dostępne</dt><dd>${b.availableCopies}/${b.totalCopies}</dd>
            </dl>
            <div class="formrow">
                <button class="btn btn--seal" data-act="borrow" data-id="${b.id}" ${out ? "disabled" : ""}>Wypożycz</button>
                <button class="btn btn--gold" data-act="reserve" data-id="${b.id}" ${out ? "disabled" : ""}>Rezerwuj</button>
            </div>
        </div>`;
        const close = () => { host.classList.add("hidden"); host.innerHTML = ""; };
        $(".modal__close", host).addEventListener("click", close);
        host.addEventListener("click", e => { if (e.target === host) close(); });
        $(".modal", host).addEventListener("click", async e => {
            const btn = e.target.closest("[data-act]"); if (!btn || btn.disabled) return;
            btn.disabled = true;
            if (btn.dataset.act === "borrow") { await doBorrow(b.id); close(); }
            if (btn.dataset.act === "reserve") { await doReserve(b.id); btn.disabled = false; }
        });
    } catch (err) {
        host.innerHTML = `<div class="modal"><button class="modal__close">×</button><div class="empty">${esc(err.message)}</div></div>`;
        $(".modal__close", host).addEventListener("click", () => { host.classList.add("hidden"); host.innerHTML = ""; });
    }
}

function fmtDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    if (isNaN(d)) return "—";
    return d.toLocaleDateString("pl-PL", { day: "2-digit", month: "2-digit", year: "numeric" });
}


(function embers() {
    const field = $("#emberfield");
    for (let i = 0; i < 26; i++) {
        const e = document.createElement("div");
        e.className = "ember";
        e.style.left = Math.random() * 100 + "vw";
        e.style.setProperty("--drift", (Math.random() * 80 - 40) + "px");
        e.style.animationDuration = (9 + Math.random() * 12) + "s";
        e.style.animationDelay = (-Math.random() * 18) + "s";
        const s = 1 + Math.random() * 2.4;
        e.style.width = e.style.height = s + "px";
        field.appendChild(e);
    }
})();


if (state.token && state.user) enterApp();
document.addEventListener("keydown", e => {
    if (e.key === "Escape") { const h = $("#modalHost"); if (!h.classList.contains("hidden")) { h.classList.add("hidden"); h.innerHTML = ""; } }
});
