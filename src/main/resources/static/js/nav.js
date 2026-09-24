// Navbar menus are <details> elements (they work without this script).
// This adds the behaviour people expect from menus: only one open at a time,
// and closing on an outside click or the Escape key.
(() => {
    "use strict";

    const menus = [...document.querySelectorAll("details.nav-menu")];

    const closeAll = (except) => {
        menus.forEach((menu) => {
            if (menu !== except) {
                menu.open = false;
            }
        });
    };

    menus.forEach((menu) => {
        menu.addEventListener("toggle", () => {
            if (menu.open) {
                closeAll(menu);
            }
        });
    });

    document.addEventListener("click", (event) => {
        if (!event.target.closest("details.nav-menu")) {
            closeAll(null);
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            const open = menus.find((menu) => menu.open);
            if (open) {
                open.open = false;
                open.querySelector("summary").focus();
            }
        }
    });
})();
