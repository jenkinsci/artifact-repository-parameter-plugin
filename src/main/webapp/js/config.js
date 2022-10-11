/**
 * These scripts are used in config.jelly
 *
 * Depending on the selection of the parameter type in the API Options section different blocks
 * are displayed/hidden.
 */

function changeOptionsVisibility(option) {
    const selection = jQuery3(option).val();
    const parentSection = jQuery3(option).parents("div.jenkins-section");

    const path = jQuery3(parentSection).find("div.arpApiOptionPath");
    const version = jQuery3(parentSection).find("div.arpApiOptionVersion");
    const repo = jQuery3(parentSection).find("div.arpApiOptionRepository");

    switch (selection) {
        case "path":
            path.show();
            version.hide();
            repo.hide();
            break;
        case "version":
            path.show();
            version.show();
            repo.hide();
            break;
        case "repository":
            path.hide();
            version.hide();
            repo.show();
    }
}

jQuery3("div.arpParamType input.jenkins-radio__input").click(function() {
    changeOptionsVisibility(this)
});

(function ($) {
    const checkedOptions = jQuery3("div.arpParamType input.jenkins-radio__input:checked");
    checkedOptions.each(function(index, element) {
        changeOptionsVisibility(element)
    });
})(jQuery3);
