/**
 * This script is used in index.jelly
 *
 * It takes the selected value & key and adds it to a hidden textarea that is used as the actual
 * value of the parameter.
 */

/**
 * Seperate multiple selected entries by the following character
 */
const delimiterEntries = '\n';
/**
 * Seperate key & value of the selected entry by the following character
 */
const delimiterTerms = ';'


function setValues(element) {
    if (!element) {
        return;
    }

    let valueField = _valueField(element);
    if (!valueField) {
        return;
    }

    let values = [];

    if (element instanceof HTMLInputElement) {
        values = _inputValues(element);
    } else if (element instanceof HTMLSelectElement) {
        values = _selectValues(element);
    }

    if (valueField) {
        valueField.value = '';
        values.forEach(val => {
            valueField.value += (val + delimiterEntries);
        });
    }
}

/**
 * Get the value field identified by type textarea and with same div#name=parameter parent.
 */
function _valueField(element) {
    const parent = element.closest("div[name=parameter]");
    if (!parent) {
        return;
    }
    return parent.querySelector("textarea[name=value]");
}

/**
 * Get the value of the key attribute of all selected <select> elements and return them as an array.
 */
function _selectValues(element) {
    let values = [];

    if (!element) {
        return values;
    }

    for (let i = 0; i < element.options.length; i++) {
        if (element.options[i].selected) {
            const key = element.options[i].getAttribute('key');
            if (key) {
                const value = element.options[i].getAttribute('value')
                if (value) {
                    values.push(value + delimiterTerms + key);
                }
            }
        }
    }

    return values;
}

/**
 * Get the values of the key attributes of all checked <input> elements within the fieldset and return them as an array.
 *
 * Function doesn't work with any version of IE!
 */
function _inputValues(element) {
    let values = [];

    if (!element) {
        return values;
    }

    const parent = element.closest("fieldset");
    if (!parent) {
        return;
    }

    const elements = parent.querySelectorAll('input');
    if (!elements) {
        return;
    }

    elements.forEach(item => {
        if (item.checked) {
            const key = item.getAttribute('key');
            if (key) {
                const value = item.getAttribute('value');
                if (value) {
                    values.push(value + delimiterTerms + key);
                }
            }
        }
    });

    return values;
}
