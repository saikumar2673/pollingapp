document.addEventListener("click", (event) => {
    const row = event.target.closest("[data-href]");
    if (row && !event.target.closest("a, button, input, select, textarea, form")) {
        window.location.href = row.dataset.href;
        return;
    }

    const addButton = event.target.closest("[data-add-option]");
    if (addButton) {
        const options = document.querySelector("#options");
        const row = document.createElement("div");
        row.className = "option-input";
        row.innerHTML = '<input name="options" required placeholder="Option label"><button type="button" class="danger secondary" data-remove-option>Remove</button>';
        options.appendChild(row);
    }

    const removeButton = event.target.closest("[data-remove-option]");
    if (removeButton) {
        const rows = document.querySelectorAll(".option-input");
        if (rows.length > 2) {
            removeButton.closest(".option-input").remove();
        }
    }
});

document.addEventListener("keydown", (event) => {
    const row = event.target.closest("[data-href]");
    if (!row || (event.key !== "Enter" && event.key !== " ")) {
        return;
    }
    event.preventDefault();
    window.location.href = row.dataset.href;
});
