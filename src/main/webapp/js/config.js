/**
 * This function is used in config.jelly
 *
 * Depending on the selection of the parameter type in the API Options section different
 * options are displayed/hidden.
 */
function showOptions(element) {
    const value = element.value;
    const parent = element.closest("div.jenkins-section");
    const pathOption = parent.querySelector("div.arpApiOptionPath");
    const versionOption = parent.querySelector("div.arpApiOptionVersion");
    const repoOption = parent.querySelector("div.arpApiOptionRepository");

    if (value === "path") {
        pathOption.style.display = "block";
        versionOption.style.display = "none";
        repoOption.style.display = "none";
    } else if (value === "version") {
        pathOption.style.display = "block";
        versionOption.style.display = "block";
        repoOption.style.display = "none";
    } else {
        pathOption.style.display = "none";
        versionOption.style.display = "none";
        repoOption.style.display = "block";
    }
}
