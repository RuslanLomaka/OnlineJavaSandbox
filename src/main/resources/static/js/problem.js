// Shows or hides the problem hint when the user clicks the hint button.
const hintButton = document.getElementById("hintButton");
const hintText = document.getElementById("hintText");

if (hintButton && hintText) {
    hintButton.addEventListener("click", () => {
        const isHidden = hintText.style.display === "none";

        hintText.style.display = isHidden ? "block" : "none";
        hintButton.textContent = isHidden ? "Hide Hint" : "Show Hint";
    });
}