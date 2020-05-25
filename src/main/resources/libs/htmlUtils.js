
function tooltipText(displayText, infoText, position = 'top') {
    var tooltipElm = document.createElement('div');
    tooltipElm.classList.add('tooltip');
    tooltipElm.innerText += displayText;

    var tooltipInfoElm = document.createElement('div');
    tooltipInfoElm.classList.add('tooltiptext');
    var positionName = position.toLowerCase();
    switch(positionName) {
        case "top":
        case "bottom":
        case "left":
        case "right":
            tooltipInfoElm.classList.add('tooltip_' + positionName);
            break;
    }

    var rID = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER);
    tooltipInfoElm.id = "tooltip_" + rID;
    tooltipInfoElm.innerText = infoText;

    tooltipInfoElm.style.marginLeft = -(tooltipInfoElm.clientWidth / 2) + 'px';
    if(tooltipInfoElm.getBoundingClientRect().x < 0)
        tooltipInfoElm.style.marginLeft = -((tooltipInfoElm.clientWidth / 2) + tooltipInfoElm.getBoundingClientRect().x) + 'px';

    tooltipElm.appendChild(tooltipInfoElm);
    return tooltipElm;
}

function border3D(elm, col = 'gray'){
    if(typeof elm != 'object') return;

    elm.style.borderColor = col;
    elm.style.borderWidth = '2px';
    elm.style.borderStyle = 'solid';

    elm.style.borderTopWidth = '1px';
    elm.style.borderLeftWidth = '1px';
}

function borderi3D(elm, col = 'gray'){
    if(typeof elm != 'object') return;

    elm.style.borderColor = col;
    elm.style.borderWidth = '2px';
    elm.style.borderStyle = 'solid';

    elm.style.borderBottomWidth = '1px';
    elm.style.borderRightWidth = '1px';
}

function uniqueRandom(checker, maxLength = 9) {
  let rngID = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER).toString();

  if (maxLength > 0 && rngID.length != maxLength)
    rngID = parseInt(rngID.substring(0, maxLength));
  rngID = parseInt(rngID);

  if (typeof checker == 'function') {
    while (true) {
      var checkID = checker(rngID);
      if (checkID >= 0) {
        rngID = checkID;
//        console.info('number approved');
        break;
      } else {
        rngID = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER).toString();
        if (maxLength > 0 && rngID.length != maxLength)
          rngID = parseInt(rngID.substring(0, maxLength));
        rngID = parseInt(rngID);
//        console.info('looping');
      }
    }
  }
  return rngID;
}

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/random
function randomInt(min, max = Number.MAX_SAFE_INTEGER, exclusive = false) {
    min = Math.ceil(min);
    max = Math.floor(max);
    if(exclusive)
        return Math.floor(Math.random() * (max - min)) + min; //The maximum is exclusive and the minimum is inclusive
    else
        return Math.floor(Math.random() * (max - min + 1)) + min; //The maximum is inclusive and the minimum is inclusive
}

function randomFloat(min, max) {
  return Math.random() * (max - min) + min;
}

function randomBool() {
  return Math.random() >= 0.5;
}

function randomChars(min = 5, max = 10) {
  let length = Math.max(min, Math.floor(Math.random() * max));
  let characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let charactersLength = characters.length;
  var result = '';
  for ( var i = 0; i < length; i++ ) {
     result += characters.charAt(Math.floor(Math.random() * charactersLength));
  }
  return result;
}
