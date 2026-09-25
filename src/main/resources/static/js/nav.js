// Navbar menus are <details> elements (they work without this script).
// This adds the behaviour people expect from menus: only one open at a time,
// and closing on an outside click or the Escape key. The mobile hamburger
// toggle (.navbar-toggle) is also a <details>, so it gets the outside-click/
// Escape behaviour too, but stays out of the "only one open at a time" group
// since the Data Structures/Algorithms menus live inside it on mobile.
(() => {
    "use strict";

    const menus = [...document.querySelectorAll("details.nav-menu")];
    const allDetails = [...document.querySelectorAll(".navbar details")];

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
        if (!event.target.closest(".navbar")) {
            allDetails.forEach((details) => {
                details.open = false;
            });
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            const open = allDetails.find((details) => details.open);
            if (open) {
                open.open = false;
                open.querySelector("summary").focus();
            }
        }
    });
})();
