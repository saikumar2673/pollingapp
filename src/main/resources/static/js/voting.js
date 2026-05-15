document.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-copy-share]");
    if (!button) {
        return;
    }
    const input = document.querySelector("#shareUrl");
    try {
        await navigator.clipboard.writeText(input.value);
        button.textContent = "Copied";
        setTimeout(() => button.textContent = "Copy URL", 1200);
    } catch {
        input.select();
    }
});
