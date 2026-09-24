/**
 * Returns the header Spring Security expects for CSRF protection, read from
 * the <meta> tags rendered by fragments/csrf.html. Returns an empty object
 * when the tags are absent (dev profile, where CSRF is disabled).
 *
 * Every state-changing fetch() (POST/PUT/PATCH/DELETE) must include these.
 */
function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');

    if (!token || !header) {
        return {};
    }

    return { [header.content]: token.content };
}
