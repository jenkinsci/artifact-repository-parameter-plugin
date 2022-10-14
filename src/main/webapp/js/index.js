/**
 * This script is used in index.jelly
 *
 * It takes the selected value & key and adds it to a hidden textarea that is used as the actual
 * value of the parameter.
 */

const delimiterEntries = '\n';
const delimiterTerms = ';'

function setValues(element) {
    let textarea = _valueField(element);

    let values = [];

    if (element instanceof HTMLInputElement) {
        values = _inputValues(element);
    } else if (element instanceof HTMLSelectElement) {
        values = _selectValues(element);
    }

    textarea.value = "";
    values.forEach(val => {
        textarea.value += (val + delimiterEntries);
    });
}

function _valueField(element) {
    const parent = element.closest("div[name=parameter]");
    if (!parent) {
        return;
    }
    return parent.querySelector("textarea[name=value]");
}

function _inputValues(element) {
    let values = [];

    const parent = element.closest("fieldset");
    const elements = parent.querySelectorAll('input');

    for (const item of elements) {
        if (!item.checked) continue;

        const key = item.getAttribute("key");
        const value = item.getAttribute("value");

        if (!key || !value) continue;

        values.push(value + delimiterTerms + key);
    }

    return values;
}

function _selectValues(element) {
    let values = [];

    for (const option of element.options) {
        if (!option.selected) continue;

        const key = option.getAttribute("key");
        const value = option.getAttribute("value");

        if (!key || !value) continue;

        values.push(value + delimiterTerms + key);
    }

    return values;
}
