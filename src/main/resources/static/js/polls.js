document.addEventListener("click", (event) => {
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
