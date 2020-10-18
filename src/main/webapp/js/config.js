/**
 * This function is used in config.jelly
 *
 * Depending on the selection of the parameter type in the API Options section different blocks
 * are displayed/hidden.
 *
 * First it tries to find the closest parent element of type "table". From there it tries to find
 * child elements of type "table" and attribute "blockId" and sets the visibility to either "block"
 * or "none".
 *
 * Function doesn't work with any version of IE!
 */
function showBlock(element) {
    if (!element) {
        return;
    }

    const parent = element.closest("table");
    const value = element.value;

    if (!parent || !value) {
        return;
    }

    let blocks2Show = [];
    let blocks2Hide = [];

    if (value === "repository") {
        blocks2Hide = ['artifactBlock', 'versionBlock'];
        blocks2Show = ['repoBlock'];
    } else if (value === "path") {
        blocks2Hide = ['repoBlock', 'versionBlock'];
        blocks2Show = ['artifactBlock'];
    } else {
        blocks2Hide = ['repoBlock'];
        blocks2Show = ['artifactBlock', 'versionBlock'];
    }

    blocks2Hide.forEach(item => {
        const block = parent.querySelector("table[blockId='" + item + "']");
        if (block) {
            block.style.display = "none";
        }
    });

    blocks2Show.forEach(item => {
        const block = parent.querySelector("table[blockId='" + item + "']");
        if (block) {
            block.style.display = "block";
        }
    });
}
