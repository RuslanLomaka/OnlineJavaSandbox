/*
 * Discussion page: loads threads from /api/problems/{slug}/threads, and lets
 * the signed-in user post, reply, edit, delete, react and upload screenshots.
 *
 * Security notes:
 *  - Post bodies arrive as HTML that the server already sanitized; it is
 *    sanitized again with DOMPurify before insertion (defence in depth).
 *  - Everything else coming from users (names, excerpts) is inserted with
 *    textContent, never innerHTML.
 *  - Every state-changing request sends the CSRF header from csrf.js.
 */
(() => {
    "use strict";

    // Must match com.example.onlinejava.discussion.Reaction.
    const REACTIONS = [
        { code: "thumbs_up", emoji: "👍" },
        { code: "heart", emoji: "❤️" },
        { code: "tada", emoji: "🎉" },
        { code: "smile", emoji: "😄" },
        { code: "thinking", emoji: "🤔" },
        { code: "rocket", emoji: "🚀" }
    ];

    const MAX_IMAGE_BYTES = 2 * 1024 * 1024;
    const EMOJI_DATA_URL = "/vendor/emoji-picker-element-1.29.1/data.json";

    const main = document.querySelector("main[data-problem-slug]");
    const slug = main.dataset.problemSlug;

    const threadsElement = document.getElementById("threads");
    const threadsStatus = document.getElementById("threadsStatus");
    const loadMoreButton = document.getElementById("loadMore");
    const composerTitle = document.getElementById("composerTitle");
    const composerContext = document.getElementById("composerContext");
    const composerContextText = document.getElementById("composerContextText");
    const composerCancel = document.getElementById("composerCancel");
    const composerSubmit = document.getElementById("composerSubmit");
    const composerError = document.getElementById("composerError");
    const emojiPopover = document.getElementById("emojiPopover");

    /** Composer mode: a new thread, a reply to a post, or an edit of a post. */
    let mode = { kind: "new" };
    let nextPage = 0;

    const relativeTime = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });

    // ------------------------------------------------------------------
    // API helper
    // ------------------------------------------------------------------

    /**
     * Calls the JSON API and returns the parsed body (or null for 204).
     * Throws an Error with a user-friendly message on failure.
     */
    async function api(path, options = {}) {
        const headers = { Accept: "application/json", ...csrfHeaders(), ...options.headers };
        if (options.json !== undefined) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(path, {
            method: options.method || "GET",
            headers,
            body: options.json !== undefined ? JSON.stringify(options.json) : options.body,
            credentials: "same-origin"
        });

        if (response.status === 204) {
            return null;
        }
        if (response.status === 401) {
            throw new Error("Your session has expired. Please reload the page and sign in again.");
        }
        if (!response.ok) {
            let message = `Request failed (${response.status})`;
            try {
                const problem = await response.json();
                if (problem.detail) {
                    message = problem.detail;
                } else if (response.status === 400) {
                    message = "Please check your input (messages must be 1–10,000 characters).";
                }
            } catch (ignored) {
                // Non-JSON error body; keep the generic message.
            }
            throw new Error(message);
        }
        return response.json();
    }

    // ------------------------------------------------------------------
    // Composer (EasyMDE Markdown editor)
    // ------------------------------------------------------------------

    const editor = new EasyMDE({
        element: document.getElementById("composerInput"),
        autoDownloadFontAwesome: false,
        spellChecker: false,
        status: false,
        minHeight: "140px",
        placeholder: "Share an idea or ask a question… Markdown, code blocks, emoji and screenshots are supported.",
        // Screenshots: paste, drag & drop, or the toolbar button.
        uploadImage: true,
        imageAccept: "image/png, image/jpeg, image/gif",
        imageMaxSize: MAX_IMAGE_BYTES,
        imageUploadFunction: uploadImage,
        imageTexts: { sbInit: "", sbOnDragEnter: "", sbOnDrop: "", sbProgress: "", sbOnUploaded: "" },
        renderingConfig: {
            // Preview is rendered in the browser; sanitize it like real posts.
            sanitizerFunction: (html) => DOMPurify.sanitize(html),
            codeSyntaxHighlighting: false
        },
        toolbar: [
            "bold", "italic", "strikethrough", "|",
            "code",
            {
                name: "java-block",
                action: insertJavaBlock,
                className: "fa-solid fa-file-code",
                title: "Insert Java code block"
            },
            "quote", "unordered-list", "ordered-list", "|",
            "link", "upload-image",
            {
                name: "emoji",
                action: toggleEmojiPicker,
                className: "fa-solid fa-face-smile",
                title: "Insert emoji"
            },
            "|", "preview"
        ]
    });

    function insertJavaBlock(mde) {
        const cm = mde.codemirror;
        const selected = cm.getSelection();
        cm.replaceSelection("\n```java\n" + (selected || "") + "\n```\n");
        if (!selected) {
            const cursor = cm.getCursor();
            cm.setCursor({ line: cursor.line - 2, ch: 0 });
        }
        cm.focus();
    }

    /** EasyMDE upload hook: sends the file to the server, returns its URL. */
    async function uploadImage(file, onSuccess, onError) {
        const form = new FormData();
        form.append("file", file);
        try {
            const result = await api("/api/attachments", { method: "POST", body: form });
            onSuccess(result.url);
        } catch (error) {
            onError(error.message);
        }
    }

    // ------------------------------------------------------------------
    // Emoji picker (emoji-picker-element web component)
    // ------------------------------------------------------------------

    let emojiPicker = null;

    function toggleEmojiPicker() {
        if (!emojiPicker) {
            emojiPicker = document.createElement("emoji-picker");
            emojiPicker.dataSource = EMOJI_DATA_URL;
            emojiPicker.classList.add("dark");
            emojiPicker.addEventListener("emoji-click", (event) => {
                editor.codemirror.replaceSelection(event.detail.unicode);
                closeEmojiPicker();
            });
            emojiPopover.appendChild(emojiPicker);
        }
        if (emojiPopover.hidden) {
            emojiPopover.hidden = false;
        } else {
            closeEmojiPicker();
        }
    }

    /**
     * Hides the picker and returns keyboard focus to the editor. Focus must be
     * moved explicitly: otherwise it stays inside the hidden picker, whose
     * keyboard navigation would swallow what the user types next.
     */
    function closeEmojiPicker() {
        emojiPopover.hidden = true;
        if (emojiPopover.contains(document.activeElement)
            || (emojiPicker && emojiPicker.shadowRoot
                && emojiPicker.shadowRoot.activeElement)) {
            document.activeElement.blur();
        }
        setTimeout(() => editor.codemirror.focus(), 0);
    }

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !emojiPopover.hidden) {
            closeEmojiPicker();
        }
    });

    document.addEventListener("click", (event) => {
        const inToolbar = event.target.closest(".editor-toolbar");
        if (!emojiPopover.hidden && !emojiPopover.contains(event.target) && !inToolbar) {
            closeEmojiPicker();
        }
    });

    // ------------------------------------------------------------------
    // Composer modes: new thread / reply / edit
    // ------------------------------------------------------------------

    function setMode(newMode) {
        mode = newMode;
        composerError.textContent = "";

        if (mode.kind === "new") {
            composerTitle.textContent = "Start a new thread";
            composerContext.hidden = true;
            composerSubmit.textContent = "Post";
            return;
        }

        composerContext.hidden = false;
        if (mode.kind === "reply") {
            composerTitle.textContent = "Reply";
            composerContextText.textContent =
                `Replying to @${mode.post.author.login}: ${excerpt(mode.post)}`;
            composerSubmit.textContent = "Post reply";
        } else {
            composerTitle.textContent = "Edit your post";
            composerContextText.textContent = "Editing your post";
            composerSubmit.textContent = "Save changes";
            editor.value(mode.post.markdown || "");
        }
        document.querySelector(".composer").scrollIntoView({ behavior: "smooth", block: "center" });
        editor.codemirror.focus();
    }

    function excerpt(post) {
        const text = new DOMParser().parseFromString(post.bodyHtml, "text/html").body.textContent;
        const flat = text.replace(/\s+/g, " ").trim();
        return flat.length > 80 ? flat.slice(0, 80) + "…" : flat;
    }

    composerCancel.addEventListener("click", () => {
        if (mode.kind === "edit") {
            editor.value("");
        }
        setMode({ kind: "new" });
    });

    composerSubmit.addEventListener("click", async () => {
        const body = editor.value().trim();
        if (!body) {
            composerError.textContent = "Please write something first.";
            return;
        }

        composerSubmit.disabled = true;
        composerError.textContent = "";
        try {
            if (mode.kind === "edit") {
                await api(`/api/posts/${mode.post.id}`, { method: "PATCH", json: { body } });
            } else {
                const replyToId = mode.kind === "reply" ? mode.post.id : null;
                await api(`/api/problems/${encodeURIComponent(slug)}/posts`,
                    { method: "POST", json: { body, replyToId } });
            }
            editor.value("");
            setMode({ kind: "new" });
            await reloadThreads();
        } catch (error) {
            composerError.textContent = error.message;
        } finally {
            composerSubmit.disabled = false;
        }
    });

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /** Small DOM helper: creates an element with a class and text content. */
    function el(tag, className, text) {
        const element = document.createElement(tag);
        if (className) {
            element.className = className;
        }
        if (text !== undefined) {
            element.textContent = text;
        }
        return element;
    }

    function formatTime(iso) {
        const seconds = (new Date(iso).getTime() - Date.now()) / 1000;
        const units = [["year", 31536000], ["month", 2592000], ["day", 86400],
            ["hour", 3600], ["minute", 60]];
        for (const [unit, size] of units) {
            if (Math.abs(seconds) >= size) {
                return relativeTime.format(Math.round(seconds / size), unit);
            }
        }
        return "just now";
    }

    function renderThread(thread) {
        const article = el("article", "thread");
        article.appendChild(renderPost(thread.root, false));

        if (thread.replies.length > 0) {
            const replies = el("div", "replies");
            thread.replies.forEach((reply) => replies.appendChild(renderPost(reply, true)));
            article.appendChild(replies);
        }
        return article;
    }

    function renderPost(post, isReply) {
        const wrapper = el("div", isReply ? "post reply" : "post");
        wrapper.id = `post-${post.id}`;

        // Header: avatar, name, handle, time.
        const header = el("header", "post-header");
        if (post.author.avatarUrl) {
            const avatar = el("img", "avatar");
            avatar.src = post.author.avatarUrl;
            avatar.alt = "";
            avatar.width = 28;
            avatar.height = 28;
            avatar.referrerPolicy = "no-referrer";
            header.appendChild(avatar);
        }
        header.appendChild(el("strong", "author-name", post.author.displayName));
        header.appendChild(el("span", "author-login", `@${post.author.login}`));
        const time = el("time", "post-time", formatTime(post.createdAt));
        time.dateTime = post.createdAt;
        time.title = new Date(post.createdAt).toLocaleString();
        header.appendChild(time);
        if (post.editedAt && !post.deleted) {
            header.appendChild(el("span", "post-edited", "(edited)"));
        }
        wrapper.appendChild(header);

        // "Replying to" quote.
        if (post.replyTo) {
            const quote = el("a", "reply-quote");
            quote.href = `#post-${post.replyTo.id}`;
            quote.appendChild(el("span", "reply-quote-author", `↪ @${post.replyTo.authorLogin}`));
            quote.appendChild(el("span", "reply-quote-text",
                post.replyTo.excerpt || "[deleted message]"));
            wrapper.appendChild(quote);
        }

        // Body.
        if (post.deleted) {
            wrapper.appendChild(el("p", "post-deleted", "This message was deleted."));
            return wrapper;
        }
        const body = el("div", "post-body");
        body.innerHTML = DOMPurify.sanitize(post.bodyHtml, { ADD_ATTR: ["target"] });
        enhanceCodeBlocks(body);
        wrapper.appendChild(body);

        // Footer: reactions and actions.
        const footer = el("footer", "post-footer");
        footer.appendChild(renderReactions(post));
        footer.appendChild(renderActions(post));
        wrapper.appendChild(footer);
        return wrapper;
    }

    /** Adds syntax highlighting and a "Copy" button to every code block. */
    function enhanceCodeBlocks(container) {
        container.querySelectorAll("pre > code").forEach((code) => {
            hljs.highlightElement(code);

            const pre = code.parentElement;
            const button = el("button", "copy-button", "Copy");
            button.type = "button";
            button.addEventListener("click", async () => {
                try {
                    await navigator.clipboard.writeText(code.textContent);
                    button.textContent = "Copied!";
                } catch (error) {
                    button.textContent = "Copy failed";
                }
                setTimeout(() => { button.textContent = "Copy"; }, 1500);
            });
            pre.appendChild(button);
        });
    }

    function renderReactions(post) {
        const bar = el("div", "reactions");
        bar.setAttribute("aria-label", "Reactions");

        const byCode = new Map(post.reactions.map((r) => [r.code, r]));
        post.reactions.forEach((reaction) => {
            bar.appendChild(reactionButton(post, reaction.code, reaction.emoji,
                reaction.count, reaction.reactedByMe));
        });

        // "Add reaction" picker with the reactions not used yet.
        const unused = REACTIONS.filter((r) => !byCode.has(r.code));
        if (unused.length > 0) {
            const details = el("details", "add-reaction");
            const summary = el("summary", "", "☺+");
            summary.title = "Add reaction";
            details.appendChild(summary);
            const menu = el("div", "add-reaction-menu");
            unused.forEach((r) => {
                const option = el("button", "reaction-option", r.emoji);
                option.type = "button";
                option.title = r.code.replace("_", " ");
                option.addEventListener("click", () => toggleReaction(post, r.code, false));
                menu.appendChild(option);
            });
            details.appendChild(menu);
            bar.appendChild(details);
        }
        return bar;
    }

    function reactionButton(post, code, emoji, count, mine) {
        const button = el("button", mine ? "reaction mine" : "reaction", `${emoji} ${count}`);
        button.type = "button";
        button.setAttribute("aria-pressed", String(mine));
        button.title = mine ? "Remove your reaction" : "React";
        button.addEventListener("click", () => toggleReaction(post, code, mine));
        return button;
    }

    async function toggleReaction(post, code, mine) {
        try {
            post.reactions = await api(`/api/posts/${post.id}/reactions/${code}`,
                { method: mine ? "DELETE" : "PUT" });
            const postElement = document.getElementById(`post-${post.id}`);
            postElement.querySelector(".reactions").replaceWith(renderReactions(post));
        } catch (error) {
            alert(error.message);
        }
    }

    function renderActions(post) {
        const actions = el("div", "post-actions");

        const reply = el("button", "link-button", "Reply");
        reply.type = "button";
        reply.addEventListener("click", () => setMode({ kind: "reply", post }));
        actions.appendChild(reply);

        if (post.own) {
            const edit = el("button", "link-button", "Edit");
            edit.type = "button";
            edit.addEventListener("click", () => setMode({ kind: "edit", post }));
            actions.appendChild(edit);

            const remove = el("button", "link-button danger", "Delete");
            remove.type = "button";
            remove.addEventListener("click", async () => {
                if (!confirm("Delete this message? Replies will stay visible.")) {
                    return;
                }
                try {
                    await api(`/api/posts/${post.id}`, { method: "DELETE" });
                    await reloadThreads();
                } catch (error) {
                    alert(error.message);
                }
            });
            actions.appendChild(remove);
        }
        return actions;
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    async function loadPage(page) {
        const result = await api(`/api/problems/${encodeURIComponent(slug)}/threads?page=${page}`);
        result.threads.forEach((thread) => threadsElement.appendChild(renderThread(thread)));
        nextPage = page + 1;
        loadMoreButton.hidden = !result.hasMore;
        threadsStatus.textContent = result.totalThreads === 0
            ? "No messages yet. Start the discussion!"
            : `${result.totalThreads} thread${result.totalThreads === 1 ? "" : "s"}`;
    }

    async function reloadThreads() {
        threadsElement.replaceChildren();
        try {
            await loadPage(0);
        } catch (error) {
            threadsStatus.textContent = error.message;
        }
    }

    loadMoreButton.addEventListener("click", async () => {
        loadMoreButton.disabled = true;
        try {
            await loadPage(nextPage);
        } catch (error) {
            threadsStatus.textContent = error.message;
        } finally {
            loadMoreButton.disabled = false;
        }
    });

    reloadThreads();
})();
