package networkRouting

import kotlinx.collections.eachHasNext
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.onKeyDown

object JsUtils {
    @Suppress("unused", "EnumEntryName")
    enum class Functions(private val init: String) {
        DisableEnter_func(
            """
                function disableEnter(field) {
                    field.onkeydown = function() {return event.key != 'Enter';};
                }
            """
        ),

        PreVerifyURL_func(
            """
                function preVerifyURL(url, hasQuery) {
                    hasQuery = hasQuery || false;
                    if (url == null || url.length == 0)
                        return "{ empty }";
                    var reg_url = /^((?:[a-z]{3,9}:\/\/)?(?:(?:(?:\d{1,3}\.){3}\d{1,3})|(?:[\w.-]+\.(?:[a-z]{2,10}))))/;
                    var reg_path = /(\/[-a-z\\d%_.~+]*)*/;
                    var reg_query = /(\?(?:&?[^=&]*=[^=&]*)*)?/;
                    var regex = new RegExp(reg_url.source + reg_path.source + (hasQuery? reg_query.source : ''), 'i');
                    var match = url.match(regex);
                    if (match == null)
                        return "{ no match }"; else return match[0];
                }
            """
        ),

        pasteAtSection_func(
            """
                function getPasteResult(event) {
                    var targetData = event.target;
                    var paste = event.clipboardData.getData('text');
                    var newText = ""
                    if (targetData.selectionStart || targetData.selectionStart == '0') {
                        var startPos = targetData.selectionStart;
                        var endPos = targetData.selectionEnd;
                        newText = targetData.value.substring(0, startPos)
                            + paste
                            + targetData.value.substring(endPos, targetData.value.length);
                    } else
                        newText = paste;
                    return newText;
                }
            """
        ),

        extractQueryFromUrl_func(
            """
            function extractQueryFromURL(url, asArray) {
                asArray = asArray || false;
                var partA = preVerifyURL(url);
                var partB = preVerifyURL(url, true);
                if (partA.length == partB.length) return (asArray ? [] : "");
                
                var query = partB.replace(partA, '').replace('?','').replace(/=/g, ' : ');
                
                if (asArray)
                    return query.split('&');
                else 
                    return query.replace(/&/g,'\n');
            }
        """
        ),

        PrettyJson(
            """
                function prettyJson(uglyText) {
                    if (uglyText.trim().length < 1) return uglyText;
                    try {
                        var obj = JSON.parse(uglyText);
                        return JSON.stringify(obj, undefined, 4);
                    }
                    catch(err) {
                        return uglyText;
                    }
                }

                function beautifyField(field) {
                    field.value = prettyJson(field.value);
                    field.style.height = (field.scrollHeight - 4) + 'px';
                }
            """
        ),

        SetIsDisabled_func(
            """
                function setIsDisabled(divObj, newState) {
                    try {
                        divObj.disabled = newState;
                    } catch (E) {}
    
                    for (var x = 0; x < divObj.childNodes.length; x++) {
                        setIsDisabled(divObj.childNodes[x], newState);
                    }
                }
            """
        ),

        FormatParentFieldWidth_func(
            """
                function formatParentFieldWidth(field) {
                    field.style.width = "100%";
    
                    var isEditing = false;
                    function adjustFullWidth() {
                        field.onmousedown = function() { isEditing = true; }
                        field.onmouseup = function() { isEditing = false; }
                        field.onmousemove = function() {
                            if (isEditing) {
                                field.parentElement.width = field.clientWidth;
                            }
                        }
                    }
                    adjustFullWidth();
                    new ResizeObserver(adjustFullWidth).observe(field);
                }
            """
        ),

        CreateTextInput_func(
            """
                function createTextInput(fieldType, fieldID, expandable) {
                    expandable = expandable || false;
    
                    var inputField = inputField = document.createElement("input");
                    if (expandable) {
                        inputField = document.createElement("textarea");
                        inputField.onkeypress = keypressNewlineEnter(inputField);
                    }
                    inputField.name = fieldType + fieldID;
                    inputField.id = inputField.name;
                    return inputField;
                }
            """
        ),

        /**
         * On the [field], hitting the 'Enter' key will add a new line
         */
        KeyPressNewlineEnter_func(
            """
                function keypressNewlineEnter(field) {
                    if (event.key == 'Enter') {
                        var pre = field.value.substring(0, field.selectionStart);
                        var post = field.value.substring(field.selectionStart, field.textLength);
                        field.value = pre + "\n" + post;
                        field.style.height = field.scrollHeight + 'px';
                        event.preventDefault();
                    }
                }
            """
        ),

        CreateCheckbox_func(
            """
                function createCheckbox(fieldType, fieldID) {
                    var inputField = document.createElement("input");
                    inputField.name = fieldType + fieldID;
                    inputField.type = "checkbox";
                    return inputField;
                }
            """
        ),

        /**
         * Created a delete button, which when clicked,
         * will call "remove()" on the passed in node
         */
        CreateDeleteBtn_func(
            """
                function createDeleteBtn(node) {
                    var deleteBtn = document.createElement("button");
                    deleteBtn.type = "button";
                    deleteBtn.innerText = "Delete";
                    deleteBtn.onclick = function() { node.remove() };
                    return deleteBtn;
                }
            """
        ),

        CreateBtn_func(
            """
                function createBtn(name) {
                    name = name || "";
                    var newBtn = document.createElement("button");
                    newBtn.type = "button";
                    newBtn.innerText = name;
                    return newBtn;
                }
            """
        ),

        ToggleDisp_func(
            """
                function toggleView(caller, toToggle) {
                    if (!toToggle.classList.contains("hideableContent")) {
                        toToggle.classList.add("hideableContent");
                        caller.classList.remove("active");
                    } else {
                        caller.classList.toggle("active");
                        var toggleStyle = toToggle.style;
                        toggleStyle.overflow = "hidden";
                        if (toggleStyle.maxHeight){
                            toggleStyle.display = "none";
                            toggleStyle.maxHeight = null;
                        } else {
                            toggleStyle.display = "inline-table";
                            toggleStyle.height = "100%"
                            toggleStyle.maxHeight = (toToggle.scrollHeight + 100) + "px";
                            var watcher = setInterval(function() {
                                if (toToggle.clientHeight == toToggle.scrollHeight) {
                                    toggleStyle.overflow = "visible";
                                    clearInterval(watcher);
                                }
                             }, 100);
                        }
                    }
                }
            """
        ),

        SetupToggButton_func(
            """
                function setupToggleButtonTarget(target) {
                    waitForElem(target, function(elem) {
                        if (!elem.classList.contains("hideableContent"))
                            elem.classList.add("hideableContent");
                        });
                }
            """
        ),

        WaitForElem_func(
            """
                function waitForElem(target, action){
                    setTimeout(function waitWrapper() {
                        var elem = document.getElementById(target);
                        if (typeof target == "object")
                            elem = target;
                        if (elem == null) setTimeout(waitWrapper, 10);
                        else action(elem);
                    }, 10);
                }
            """
        ),

        GetScriptElement_func(
            """
                function getScriptElem() {
                    var scriptTag = document.getElementsByTagName('script');
                    return scriptTag[scriptTag.length - 1];
                }
            """
        ),

        SetupToggleArea_func(
            """
                function setupToggleArea() {
                    var scriptTag = getScriptElem();
                    var togDiv = scriptTag.previousElementSibling;
                    var togBtn = togDiv.previousElementSibling.previousElementSibling;

                    togBtn.onclick = function() { toggleView(togBtn, togDiv) };
                    togDiv.classList.add("hideableContent");
                    return [togBtn, togDiv];
                }
            """
        ),

        SubmitNameCheck(
            """
                function submitCheck(checkName) {
                    checkName.value = checkName.value.trim();
                    if (checkName.value == "")
                        checkName.value = checkName.placeholder;
                }
            """
        );

        companion object {
            /**
             * Returns a string containing all the values in [Functions]
             */
            val all: String
                get() {
                    val result = StringBuilder()
                    values().asList().eachHasNext(
                        { result.append(it.value) },
                        { result.append('\n') }
                    )
                    return result.toString()
                }
        }

        val value: String
            get() = init.trimIndent()
    }

    val CommonAttributeGroupFacade.disableEnterKey: Unit
        get() {
            onKeyDown = "return event.key != 'Enter';"
        }
}
