function enableSortRoot() {
  var options = {
    group: 'level_root',
    handle: '.sjs_handle',
    ghostClass: 'sjs_ghost',
    filter: '.sjs_noDrag',
    preventOnFilter: false,
    animation: 100,
    onUpdate: function(evt) {
      console.info('Update HOST!!');
      let itemID = parseInt(evt.item.attributes.idCode.value);
      document.p4ExportData.UpdateHostOrder(itemID, evt.newIndex);
    }
  }

  Sortable.create(level_root, options);
}

function enableToggleArea(toggBtn, toggElm) {
  var backup = [toggBtn, toggElm];
  if (typeof toggBtn != 'object')
    toggBtn = document.getElementById(toggBtn);
  if (typeof toggElm != 'object')
    toggElm = document.getElementById(toggElm);
  if (toggBtn == null || toggElm == null) {
    let errorStr = 'Unable to enable toggle area';
    errorStr += '(' + backup.toString() + ')';
    errorStr += ' -> {' + [toggBtn, toggElm].toString() + '}';
    console.error(errorStr);
    return;
  }

  //  toggBtn.addEventListener('click', () => toggleView(toggBtn, toggElm));
  toggBtn.onclick = function() { toggleView(toggBtn, toggElm) };
  toggElm.classList.add('hideableContent');
}

function seqContentIDs(sID) {
  var getElem = (elm) => document.getElementById(elm);
  return {
    titleView: getElem('viewTxTitle_' + sID),
    titleEdit: getElem('editTxTitle_' + sID),
    editCancelBtn: getElem('rootEditCancel_' + sID),
    saveBtn: getElem('rootSave_' + sID),
    deleteBtn: getElem('rootDelete_' + sID),
    contentView: getElem('rootContent_' + sID)
  }
}

function toggleEditButton(sID) {
  let seqIDs = seqContentIDs(sID);
  if (seqIDs.contentView.style.display == 'none') {
    seqIDs.titleView.style.display = 'none';
    seqIDs.titleEdit.style.display = 'block';

    seqIDs.editCancelBtn.innerHTML = 'Cancel';
    seqIDs.saveBtn.style.display = 'inline';
    seqIDs.deleteBtn.style.display = 'inline';

    seqIDs.contentView.style.display = 'block';
  } else {
    seqIDs.titleView.style.display = 'block';
    seqIDs.titleEdit.style.display = 'none';

    seqIDs.editCancelBtn.innerHTML = 'Edit';
    seqIDs.saveBtn.style.display = 'none';
    seqIDs.deleteBtn.style.display = 'none';

    seqIDs.contentView.style.display = 'none';
  }
}

// Enable re-ordering for child items
function enableSortLevel(levelElem) {
  var options = {
    group: 'level_1',
    handle: '.sjs_handle',
    ghostClass: 'sjs_ghost',
    animation: 100,
    onUpdate: function(evt) {
      console.info('Update ITEM!!');
      let childID = parseInt(evt.item.attributes.idCode.value);
      let hostID = parseInt(evt.from.attributes['idcode'].value);
      document.p4ExportData.UpdateItemHost(childID, hostID, evt.newIndex - 1);
    },
    onAdd: function(evt) {
      console.info('Added ITEM!!');
      let childID = parseInt(evt.item.attributes.idCode.value);
      let hostID = parseInt(evt.to.attributes['idcode'].value);
      document.p4ExportData.UpdateItemHost(childID, hostID, evt.newIndex - 1);
    },
    onChange: function(evt) {
      // enable/disable the "ready for new items" when moving between parents
      flipperReadyDiv(evt.from);
      flipperReadyDiv(evt.to);
    },
    onRemove: function(evt) {
      //      console.log({
      //        'event': 'onRemove',
      //        'this': this,
      //        'item': evt.item,
      //        'from': evt.from,
      //        'to': evt.to,
      //        'oldIndex': evt.oldIndex,
      //        'newIndex': evt.newIndex
      //      });
    }
  }

  levelElem.attributes['sortable'] = options;
  Sortable.create(levelElem, options);
}

// Enables/ disables the "Ready for elements" div when a child element is moved
function flipperReadyDiv(parentElm) {
  let seqItems = parentElm.querySelectorAll('[idCode]');
  let readyDiv = parentElm.querySelector('[class=readyDiv]');
  if (readyDiv == null) return;
  readyDiv = readyDiv.style;
  if (seqItems.length > 0)
    readyDiv.display = 'none';
  else
    readyDiv.display = '';
}

function getElementById(parent, id) {
  return parent.querySelector('[id=' + id + ']');
}

function getKeyByValue(object, value) {
  return Object.keys(object).find(key => object[key] === value);
}

function appendOption(selectElm, text, select = false) {
  let opt = selectElm.appendChild(document.createElement('option'));
  opt.innerText = text;
  opt.selected = select;
}

function respondToVisibility(element, callback) {
  //  https://stackoverflow.com/a/44670818
  var options = {
    root: document.documentElement
  }

  var observer = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      callback(entry.intersectionRatio > 0);
    });
  }, options);

  observer.observe(element);
}

function createHiddenInput(name, value) {
  let hiddenInput = document.createElement('input');
  hiddenInput.type = 'hidden';
  hiddenInput.name = name;
  hiddenInput.value = value;
  return hiddenInput;
}

/*
Math.floor(Math.random() * Number.MAX_SAFE_INTEGER)

events = [
  'onAdd',
  'onSort',
  'onUpdate',
  'onRemove'
].forEach(function(name) {
  options[name] = function(evt) {
    console.log({
      'event': name,
      'this': this,
      'item': evt.item,
      'from': evt.from,
      'to': evt.to,
      'oldIndex': evt.oldIndex,
      'newIndex': evt.newIndex
    });
  };
});
*/

//function styleObserver(target, styles, callback) {
//if (!Array.isArray(styles)) return;
//if (typeof callback != 'function') return;
////var observer = new MutationObserver(callback);
//var observer = new MutationObserver(function(mutations) {
//    mutations.forEach(function(mutationRecord) {
//        console.log('style changed!');
//    });
//});
//
//observer.observe(target, { attributes : true, attributeFilter : ['style'] });
//}
//
//styleObserver($0, 'background-color', function() {
//  console.info(arguments);
//});

class p4Parser {
  constructor(inData) {
    if (inData == null || typeof inData != 'object' || !inData.isValid)
      inData = {};
    this.emptyData = Object.keys(inData).length == 0;

    this.isCond = inData.isCond || false;
    this.isOpt = inData.isOpt || false;
    this.condSrc = inData.condSrc || 0;

    this.srcType = inData.srcType || 0;
    this.srcRType = inData.srcRType || 0;
    this.isHead = inData.isHead || false;
    this.isBody = inData.isBody || false;
    this.varLevel = inData.varLevel || 0;
    this.varSearchUp = inData.varSearchUp || false;

    this.source_name = inData.source_name || "";
    this.source_match = inData.source_match || "";

    this.hasAction = inData.hasAction || false;
    this.act_scopeLevel = inData.act_scopeLevel || false;
    this.act_name = inData.act_name || "";
    this.act_nExists = inData.act_nExists || false;
    this.act_nCount = inData.act_nCount || false;
    this.act_nResult = inData.act_nResult || false;
    this.act_nSpread = inData.act_nSpread || false;
    this.act_nSpreadType = inData.act_nSpreadType || -8;
    this.act_match = inData.act_match || "";

    this.zData = "";
  }

  Clone() {
    return Object.assign(new p4Parser(), this);
  }

  get source_hasItems() {
    return (this.source_name || '').length > 0 || (this.source_match || '').length > 0;
  }

  get act_hasItems() {
    return (this.act_name || '').length > 0 || (this.act_match || '').length > 0;
  }

  Result() {
    var output = '';
    var valid = true;

    if (this.isCond) {
      if (this.isOpt) {
        output += '~';
      }
      switch (this.condSrc) {
        case 1:
          output += '?';
          break;
        case 2:
          output += '!';
          break;
      }
    }

    switch (this.srcType) {
      case 0:
        return 'Missing Source type';
        break;

      case 1:
        output += 'request:';
        if (this.isHead) {
          output += 'head';
        } else if (this.isBody) {
          output += 'body';
        } else
          return 'Missing sub-source type';
        break;

      case 2:
        output += 'response:';
        if (this.isHead) {
          output += 'head';
        } else if (this.isBody) {
          output += 'body';
        } else
          return 'Missing sub-source type';
        break;

      case 3:
        switch (this.varLevel) {
          case 1:
            output += '&';
            break;
          case 2:
            output += '%';
            break;
        }
        if (this.varSearchUp)
          output += '^';
        output += 'var';
        break;

      case 4:
        output += 'use';
        break;
    }

    if (!valid)
      return "Invalid";

    if (this.source_hasItems) {
      if ((this.source_name || '').length > 0) output += '[' + this.source_name + ']';
      if ((this.source_match || '').length > 0) output += ':{' + this.source_match + '}';
    }

    if (this.act_hasItems) {
      output += '->';
      if ((this.act_name || '').length > 0) {
        switch (this.act_scopeLevel) {
          case 1:
            output += '&';
            break;
          case 2:
            output += '%';
            break;
        }
        output += this.act_name;
        if (this.act_nExists) output += '?';
        if (this.act_nCount) output += '#';
        if (this.act_nResult) output += '@';
        if (this.act_nSpreadType != -8) {
          switch (this.act_nSpreadType) {
            case -1:
              output += '_#';
              break;
            case -2:
              output += '_?';
              break;
          }
          if (this.act_nSpreadType >= 0) output += ('_#' + this.act_nSpreadType);
        }
      } else if ((this.act_match || '').length > 0) {
        output += '{' + this.act_match + '}'
      }
    }

    this.emptyData = false;
    this.compressZData();
    return output;
  }

  compressZData() {
    var replacer = (key, value) => {
      var skipKeys = ['emptyData', 'hasAction', 'zData']
      if (skipKeys.includes(key)) return undefined;
      else return value;
    };

    switch (this.srcType) {
      case 1:
        this.isRequest = true;
        break;
      case 2:
        this.isResponse = true;
        break;
      case 3:
        this.isType_V = true;
        break;
      case 4:
        this.isType_U = true;
        break;
    }
    this.isOpt = this.cStOpt;
    this.condSrc = this.cStSrc;

    let tempData = Object.assign({}, this);
    tempData.source_name = btoa(tempData.source_name);
    tempData.source_match = btoa(tempData.source_match);
    tempData.act_name = btoa(tempData.act_name);
    tempData.act_match = btoa(tempData.act_match);

    let strData = JSON.stringify(tempData, replacer);
    LZMA.compress(strData, 6, (compStr) => {
      this.zData = convert_to_formated_hex(compStr);
    }, () => {});
  }

  shuffle() {
    this.isCond = randomBool();
    this.isOpt = randomBool();
    this.condSrc = randomInt(-1, 2);

    this.srcType = randomInt(0, 4);
    this.isHead = randomBool();
    this.isBody = randomBool();
    this.varLevel = randomInt(-1, 2);
    this.varSearchUp = randomBool();

    this.source_name = randomBool() ? randomChars() : null;
    this.source_match = randomBool() ? randomChars() : null;

    this.act_name = randomBool() ? randomChars() : null;
    this.act_nExists = randomBool();
    this.act_nCount = randomBool();
    this.act_nResult = randomBool();
    this.act_nSpread = randomBool();
    this.act_nSpreadType = randomBool() ? randomInt(-1, 30) : -8;
    this.act_scopeLevel = randomInt(-1, 2);
    this.act_match = randomBool() ? randomChars() : null;
    this.hasAction = (this.act_name != null ||
      this.act_scopeLevel > -1 || this.act_match != null);

    return this.Result;
  }
}

class parserEditor {
  constructor(hostID) {
    if (document.p4ExportData == undefined)
      document.p4ExportData = new p4DataExporter();
    this.hostID = hostID || uniqueRandom(rr => document.p4ExportData.ContainsItemID(rr) ? -1 : rr);
  }

  // created a new "blank slate" editor
  AddNewSeqList(initData, headerName = '') {
    let listRoot = document.getElementById('level_root');
    if (listRoot == null) return;

    let mainTable = this.createMainTable();
    mainTable.setAttribute('idCode', this.hostID);
    let mainBody = mainTable.querySelector('tbody')
    mainBody.appendChild(this.createHeader(headerName));
    mainBody.appendChild(this.createContent());

    let togBtn = getElementById(mainBody, 'level0ContentToggle_' + this.hostID)
    let togElm = getElementById(mainBody, 'level0ContentWrapper_' + this.hostID)
    enableToggleArea(togBtn, togElm);

    listRoot.appendChild(mainTable);
    if (initData != null)
      this.LoadSeqData(initData);
  }

  // loads a compressed host filled with items
  LoadSeqData(data) {
    document.p4ExportData.ClearAll();
    var parent = this;
    var holdingData = data;
    let cmdDataStream = convert_formated_hex_to_bytes(data.Commands);

    LZMA.decompress(cmdDataStream, (cmdItemsStr) => {
      JSON.parse(cmdItemsStr)
        .map(strObj => Object.assign(new p4Parser(), strObj))
        .forEach(command => {
          let commandContent = new parserCommand(parent.hostID);
          command.source_name = atob(command.source_name);
          command.source_match = atob(command.source_match);
          command.act_name = atob(command.act_name);
          command.act_match = atob(command.act_match);

          commandContent.SetupAsData(command);
          parent.hostDiv.appendChild(commandContent.CreateView());
          commandContent.runDataLoadUpdate();
          commandContent.UpdateEditView();
        });
      flipperReadyDiv(this.hostDiv);
    }, () => {});
  }

  createMainTable() {
    let mainTable = document.createElement('table');
    mainTable.className += 'sjs_group-item nested-1';
    mainTable.style.border = '0';
    mainTable.style.display = 'table';

    let headers = document.createElement('thead');
    mainTable.appendChild(headers);

    let headRow = document.createElement('tr');
    headers.appendChild(headRow);
    headRow.style.visibility = 'collapse';

    let headTH_1 = document.createElement('th');
    headTH_1.style.width = '1em';
    headRow.appendChild(headTH_1);
    headRow.appendChild(document.createElement('th'));

    mainTable.appendChild(document.createElement('tbody'));
    return mainTable;
  }

  createHeader(headerText = '') {
    let handleCell = document.createElement('td');
    handleCell.style.backgroundColor = 'inherit'
    handleCell.style.padding = '0px';

    let handle = document.createElement('div');
    handle.classList.add('sjs_handle');
    handle.style.paddingLeft = '8px';
    handle.style.paddingRight = '8px';
    handle.innerText = ':::';
    handleCell.appendChild(handle);

    let btnCell = document.createElement('div');
    btnCell.style.backgroundColor = 'inherit'
    btnCell.style.padding = '0px';

    let btn = document.createElement('button');
    btn.type = 'button';
    btn.className += 'inline collapsible';
    btn.id = 'level0ContentToggle_' + this.hostID;
    if (headerText == '')
      btn.innerText = 'Sequence ' + this.hostID;
    else
      btn.innerText = headerText;
    btnCell.appendChild(btn);

    let headerRow = document.createElement('tr');
    headerRow.appendChild(handleCell);
    headerRow.appendChild(btnCell);
    return headerRow;
  }

  createContent() {
    let contentCell = document.createElement('td');
    contentCell.colSpan = 2;
    contentCell.style.padding = '0';
    contentCell.style.backgroundColor = 'inherit';

    let contentToggle = document.createElement('div');
    contentToggle.id = 'level0ContentWrapper_' + this.hostID;
    contentToggle.style.display = 'none';
    contentToggle.style.padding = '0px';
    contentToggle.style.backgroundColor = 'inherit';
    contentCell.appendChild(contentToggle);

    let contentDiv = document.createElement('div');
    this.hostDiv = contentDiv;
    contentDiv.id = 'level0Content_' + this.hostID;
    contentDiv.setAttribute('idCode', this.hostID);
    contentDiv.style.backgroundColor = '#f4f4f4';
    contentDiv.style.padding = '0.4em';
    borderi3D(contentDiv);
    contentDiv.style.borderBottomWidth = '0px';
    contentDiv.style.marginBottom = '0px';
    enableSortLevel(contentDiv);

    let readyDiv = document.createElement('div');
    readyDiv.classList.add('readyDiv');
    //    readyDiv.style.width = '50%';
    //    readyDiv.style.display = 'none';
    readyDiv.style.userSelect = 'none;'
    readyDiv.style.padding = '4px';
    readyDiv.style.backgroundColor = '#f4f4f4';
    readyDiv.style.borderColor = 'gray';
    readyDiv.style.borderWidth = '2px';
    readyDiv.style.borderStyle = 'dotted';
    readyDiv.style.margin = 'auto';
    readyDiv.style.textAlign = 'center';
    readyDiv.innerText = 'Click "Add new" or drag command items here to add';
    contentDiv.appendChild(readyDiv);

    contentToggle.appendChild(contentDiv);
    contentToggle.appendChild(this.newSeqCmdBtn());

    let contentRow = document.createElement('tr');
    contentRow.appendChild(contentCell);
    return contentRow;
  }

  newSeqCmdBtn() {
    let newSeqDiv = document.createElement('div');
    newSeqDiv.style.backgroundColor = '#f4f4f4';
    newSeqDiv.style.padding = '0.5em';
    borderi3D(newSeqDiv);
    newSeqDiv.style.borderTopWidth = '0';

    let newSeqBtn = document.createElement('button');
    newSeqBtn.type = 'button';
    newSeqBtn.innerText = 'Add new command';
    newSeqBtn.addEventListener('click', () => {
      let commandContent = new parserCommand(this.hostID);
      this.hostDiv.appendChild(commandContent.CreateView());
      commandContent.UpdateEditView();
      commandContent.enableEditing();
      flipperReadyDiv(this.hostDiv);
    });
    newSeqDiv.appendChild(newSeqBtn);

    return newSeqDiv;
  }
}

class p4DataExporter {
  constructor() {
    // [hostID, order]
    this.HostOrder = {};
    /*
    {
      HostID_A: {ItemID_0: Index, ItemID_1: Index},
      HostID_B: {ItemID_2: Index, ItemID_3: Index},
    }
    */
    this.HostItems = {};
    /*
    {
      Item_A: {
        data: '',
        parserObj: {},
        Clone: () => {}
      },
      Item_B: {
        data: '',
        parserObj: {},
        Clone: () => {}
      }
    }
    */
    this.ItemArray = {};
    this.loadFromStorage();
  }

  ContainsHostID(ID) {
    return ID in this.HostItems;
  }

  ContainsItemID(ID) {
    return ID in this.ItemArray;
  }

  ClearAll() {
    this.HostOrder = {};
    this.HostItems = {};
    this.ItemArray = {};
    sessionStorage.clear();
  }

  // { hostID: [itemID, itemID] }
  getHost(hostID) {
    return this.HostItems[hostID] || null;
  }

  // `createItemObj` object
  getItem(itemID) {
    return this.ItemArray[itemID] || null;
  }

  // Adds a new Host to this collection
  AddHost(hostID) {
    if (!(hostID in this.HostItems)) {
      let hostCount = Object.keys(this.HostOrder).length;
      this.HostOrder[hostID] = (hostCount == 0) ? 0 : hostCount;
      this.HostItems[hostID] = {};
    }
    return this.HostItems[hostID];
  }

  // (optionally) Adds a new Host to this collection
  // Adds a new Item to this collection (default; empty string. Optional value)
  AddItemToHost(hostID, itemID) {
    if (hostID == null || itemID == null) return;
    if (this.ContainsHostID(hostID) && this.ContainsItemID(itemID)) return;

    let newHost = this.AddHost(hostID); // add/ collect the host
    let itemCount = Object.keys(newHost).length;
    if (!(itemID in newHost)) {
      newHost[itemID] = (itemCount == 0) ? 0 : itemCount;
      this.ItemArray[itemID] = this.createItemObj();
    }
  }

  createItemObj() {
    var selfObj = {
      data: '',
      parserObj: new p4Parser(),
      Clone: () => {
        let cloneObj = this.createItemObj();
        cloneObj.data = this.data;
        cloneObj.parserObj = this.parserObj.Clone();
        return cloneObj;
      },
      Update: (newData) => {
        if (newData.constructor.name == 'p4Parser') {
          selfObj.data = newData.Result();
          selfObj.parserObj = newData.Clone();
        }
      },
      ExportData: (key) => {
        let strData = JSON.stringify(selfObj.parserObj);
        LZMA.compress(strData, 6, (compStr) => {
          sessionStorage.setItem("item_" + selfObj.key, convert_to_formated_hex(compStr));
        }, () => {});
      }
    };
    return selfObj;
  }

  UpdateHostOrder(hostID, newIndex = -1) {
    if (!(hostID in this.HostOrder)) return; // unknown host ID

    let indexFrom = this.HostOrder[hostID];
    let indexToID = getKeyByValue(this.HostOrder, newIndex);

    this.HostOrder[hostID] = newIndex;
    this.HostOrder[indexToID] = indexFrom;
  }

  // Updates the Data for the selected Item
  UpdateItemData(itemID, data) {
    if (!(itemID in this.ItemArray)) return;
    switch (data.constructor.name) {
      case 'String':
        this.ItemArray[itemID].data = data;
        break;
      case 'p4Parser':
        this.ItemArray[itemID].parserObj = data;
        this.ItemArray[itemID].data = data.Result();
        break;
    }
  }

  // Remove the Item from an old host (if added to one)
  // Then add the Item to the new host
  // if newItemIndex == -1, then add to the end
  UpdateItemHost(itemID, newHostID, newItemIndex = -1) {
    if (!(itemID in this.ItemArray)) return; // unknown item ID

    let i;
    let oldHost = Object.keys(this.HostItems)
      .map(hostKey => this.HostItems[hostKey])
      .filter(hostObj => itemID in hostObj)[0];
    if (oldHost != null) {
      let oldIndex = oldHost[itemID];
      delete oldHost[itemID];

      // == Move all relevant index items down to fill in the index gap
      let oldHostKeys = Object.keys(oldHost);
      let oldHostCount = oldHostKeys.length;

      /*
      item: index
      34: 0
      56: 1
      22: 2 -> oldIndex
      35: 3

      34: 0
      56: 1
      35: 3
      */
      // Shift all the values greater than oldIndex down one
      for (i = 0; i < oldHostCount; i++) {
        let nextItemKey = oldHostKeys[i];
        let nextItemValue = oldHost[nextItemKey];
        if (nextItemValue > oldIndex)
          oldHost[nextItemKey] = nextItemValue - 1;
      }
    }

    // == Move all relevant index items up, to make room for the new item
    let newHost = this.AddHost(newHostID);
    let newHostKeys = Object.keys(newHost);
    let newHostCount = newHostKeys.length;

    for (i = 0; i < newHostCount; i++) {
      let nextItemKey = newHostKeys[i];
      let nextItemValue = newHost[nextItemKey];
      if (nextItemValue >= newItemIndex && newItemIndex > -1)
        newHost[nextItemKey] = nextItemValue + 1;
    }
    if (newItemIndex == -1)
      newHost[itemID] = newHostCount;
    else
      newHost[itemID] = newItemIndex;
  }

  // save items to storage, so we can retrieve the raw data on refresh
  saveToStorage() {}
  // old version, saved for testing
  saveToStorage_1() {
    sessionStorage.clear();
    Object.entries(this.HostItems).map(([key, data]) =>
      sessionStorage.setItem("host_" + key, JSON.stringify(data))
    );

    Object.entries(this.ItemArray).map(([key, data]) => {
      // sessionStorage.setItem("item_" + key, JSON.stringify(data.parserObj));
      data.ExportData(key);
    });
  }

  loadFromStorage() {}
  // old version, saved for testing
  loadFromStorage_1() {
    Object.entries(sessionStorage).filter(entry => entry[0].includes('_'))
      .forEach(([keyStr, data]) => {
        var keySplit = keyStr.split('_');
        var keyID = keySplit[1];

        switch (keySplit[0]) {
          case 'host':
            this.HostItems[keyID] = JSON.parse(data);
            break;
          case 'item':
            this.ItemArray[keyID] = this.createItemObj();

            LZMA.decompress(data, (cmdItemsStr) => {
              let loadData = Object.assign(new p4Parser(), JSON.parse(cmdItemsStr));
              this.ItemArray[keyID].Update(loadData);
            }, () => {});
            break;
        }
      });
  }

  toString() {
    let result = "{";
    result += `"hostOrder": ${JSON.stringify(this.HostOrder)},`;
    result += `"hosts": ${JSON.stringify(this.HostItems)},`;

    let itemArrayData = Object.entries(this.ItemArray).map(([key, itemData]) => ({
      ID: key,
      Data: btoa(itemData.data),
      ZData: itemData.parserObj.zData || ''
    }));
    result += `"items": ${JSON.stringify(itemArrayData)}`;
    result += "}";
    return result;
  }
}

class parserCommand {
  constructor(hostID, itemID) {
    this.initialData = '';
    this.hostID = hostID;
    this.itemID = itemID || uniqueRandom(rr => document.p4ExportData.ContainsItemID(rr) ? -1 : rr);

    document.p4ExportData.AddItemToHost(this.hostID, this.itemID);

    this.itemData = document.p4ExportData.getItem(this.itemID);
    this.cmdData = this.itemData.parserObj;

    let cellInit = this.editorInit();
    this.cellIDs = cellInit['cellIDs'];
    this.bindings = cellInit['binders'];
    this.dataLoadUpdaters = [];
  }

  CreateView() {
    let itemDiv = document.createElement('div');
    this.thisView = itemDiv;
    itemDiv.setAttribute('idCode', this.itemID);
    border3D(itemDiv);
    itemDiv.style.marginTop = '4px';
    itemDiv.style.marginBottom = '4px';

    if (this.initialData.length > 0)
      itemDiv.setAttribute('initial', this.initialData);

    itemDiv.appendChild(this.createTitleTable());
    itemDiv.appendChild(this.createCmdEditor());

    return itemDiv;
  }

  // loads "dataObj" (json data) into this editor
  SetupAsData(dataObj) {
    this.itemData.Update(dataObj);
    this.cmdData = this.itemData.parserObj;
    this.initialData = this.itemData.data;

    this.dataLoadUpdaters = [];
    this.bindings.isCond.load(dataObj.isCond);
    this.bindings.isOpt.load(dataObj.isOpt);
    this.bindings.condSrc.load(dataObj.condSrc);

    this.bindings.srcType.load(dataObj.srcType);
    this.bindings.srcRType.load(dataObj.srcRType);
    this.bindings.varLevel.load(dataObj.varLevel);

    this.bindings.source_name_enabled.load(dataObj.source_name.length != 0);
    this.bindings.source_name.load(dataObj.source_name);

    this.bindings.source_match_enabled.load(dataObj.source_match.length != 0);
    this.bindings.source_match.load(dataObj.source_match);

    this.bindings.hasAction.load(dataObj.hasAction);
    this.bindings.out_ToVar.load(dataObj.act_name.length != 0);
    this.bindings.out_ToSrc.load(dataObj.act_match.length != 0);
    this.bindings.act_scopeLevel.load(dataObj.act_scopeLevel);
    this.bindings.act_name.load(dataObj.act_name);
    this.bindings.act_nExists.load(dataObj.act_nExists);
    this.bindings.act_nCount.load(dataObj.act_nCount);
    this.bindings.act_nResult.load(dataObj.act_nResult);
    this.bindings.act_nSpread.load(dataObj.act_nSpread);
    this.bindings.act_nSpreadType.load(dataObj.act_nSpreadType);

    this.bindings.act_match.load(dataObj.act_match);

    //    this.bindings.act_match.load("1234")
    //    this.UpdateEditView();
  }

  activeData() {
    return this.editData || this.cmdData;
  }

  // returns True if the editing content is visible
  isEditMode() {
    return this.cellIDs.root.Content.Cell.style.display != 'none';
  }

  UpdateEditView() {
    if (this.titleEdit == null) return;
    let displayText = this.activeData().Result();
    document.p4ExportData.UpdateItemData(this.itemID, displayText);
    this.titleView.innerHTML = displayText;
    this.titleEdit.innerHTML = displayText;
  }

  // Setup IDs and bindings for all the available cell types
  editorInit() {
    class rootConfig {
      constructor(root, children = []) {
        this.root = root;
        this.children = children;
      }
    }

    class childConfig {
      constructor(elmName, boundName = null) {
        this.elmName = elmName;
        // internal name this object will bind to (in p4Parser)
        this.boundName = boundName;
      }
    }

    // IDs (and optional binding name)
    // Root[child] -> {root}{child}_{parent item id}
    let ElemIDConfigs = [
      new rootConfig('viewTx', [
        new childConfig('Title')
      ]),
      new rootConfig('editTx', [
        new childConfig('Title')
      ]),

      // title buttons: Delete, Save, Edit
      new rootConfig('root', [
        new childConfig('Delete'),
        new childConfig('Save'),
        new childConfig('EditCancel'),
        new childConfig('Content')
      ]),

      // Conditional
      new rootConfig('cond', [
        new childConfig('Enabled', 'isCond'),
        new childConfig('Off'),
        new childConfig('On'),
        new childConfig('Opt', 'isOpt'),
        new childConfig('ReqT', 'condSrc')
      ]),

      // Source
      new rootConfig('src', [
        new childConfig('Type', 'srcType'),
      ]),
      new rootConfig('srcRType', [
        new childConfig('Row'),
        new childConfig('Data', 'srcRType')
      ]),
      //      new rootConfig('srcVar', [
      //        new childConfig('Type'),
      //        new childConfig('Scope')
      //      ]),
      new rootConfig('srcVarScope', [
        new childConfig('Row'),
        new childConfig('Data', 'varLevel')
      ]),
      new rootConfig('srcVarSrcUp', [
        new childConfig('Row'),
        new childConfig('Data', 'varSearchUp')
      ]),
      new rootConfig('srcIName', [
        new childConfig('Row'),
        new childConfig('State', 'source_name_enabled'),
        new childConfig('Off'),
        new childConfig('On'),
        new childConfig('Data', 'source_name')
      ]),
      new rootConfig('srcIMatch', [
        new childConfig('Row'),
        new childConfig('State', 'source_match_enabled'),
        new childConfig('Off'),
        new childConfig('On'),
        new childConfig('Data', 'source_match')
      ]),

      // Action
      new rootConfig('act', [
        new childConfig('Enabled', 'hasAction'),
        new childConfig('Off'),
        new childConfig('On'),
        new childConfig('typeOption'),
        new childConfig('TypeVar', 'out_ToVar'),
        new childConfig('TypeSrc', 'out_ToSrc')
      ]),
      new rootConfig('actVar', [
        new childConfig('Row'),
        new childConfig('Scope', 'act_scopeLevel'),
        new childConfig('Data', 'act_name')
      ]),
      new rootConfig('actVarPost', [
        new childConfig('Row'),
        new childConfig('Exists', 'act_nExists'),
        new childConfig('Count', 'act_nCount'),
        new childConfig('Result', 'act_nResult'),
        new childConfig('Spread', 'act_nSpread'),
        new childConfig('IndexRow'),
        new childConfig('Index', 'act_nSpreadType')
      ]),
      new rootConfig('actMatch', [
        new childConfig('Row'),
        new childConfig('Data', 'act_match')
      ])
    ];

    let outData = {
      cellIDs: {},
      binders: {}
    };

    let makeDataObjs = (rootName, childConfig = null) => {
      let subName = childConfig?.elmName || '';
      var creator = {
        ID: `${rootName}${subName}_${this.itemID}`,
        Cell: null,
        setCell: function(elm) {
          this.Cell = elm;
          elm.id = this.ID;
          if (this.boundName !== undefined &&
            outData.binders[this.boundName].boundView != this.Cell)
            this.initCellBinding();
        },
        bindData: function() {
          if (childConfig?.boundName == null) return;
          let bindName = childConfig.boundName

          let binder = outData.binders[bindName];
          if (binder == null) { // initialize new object
            this.boundName = bindName;
            outData.binders[bindName] = {};
            binder = outData.binders[bindName];
          }

          var loadHoldingValue = null;
          binder.load = (val) => { loadHoldingValue = val };
          binder.get = () => loadHoldingValue;

          var collectElmItems = () => {
            let attribute = null;
            let event = null;
            if (this.Cell == null)
              return {
                attribute: attribute,
                event: event
              }

            switch (this.Cell.nodeName) {
              case 'INPUT':
                switch (this.Cell.type) {
                  case 'checkbox':
                    attribute = function(elm, data) {
                      elm.checked = data;
                    };
                    event = 'click';
                    break;
                  case 'radio':
                    attribute = function(elm, data) {
                      elm.checked = data;
                    };
                    event = 'change';
                    break;
                  case 'text':
                    attribute = function(elm, data) {
                      elm.value = data;
                    };
                    event = 'input';
                    break;
                }
                break;
              case 'SELECT':
                attribute = function(elm, data) {
                  elm.selectedIndex = data;
                };
                event = 'change';
                break;
              default:
                let cellToString = `ID ${this.Cell.id}`
                console.log(`Invalid cell type (${this.Cell.type}) @ ${cellToString} `)
                break;
            }

            return {
              attribute: attribute,
              event: event
            }
          };

          // setup data binding on this.Cell
          var runCellBinding = () => {
            binder.boundView = this.Cell;

            // loads a value into the UI
            binder.load = (val) => {
              if (this.parent.activeData()[bindName] != undefined)
                this.parent.activeData()[bindName] = val;
              //            else
              //                console.log(`Unknown var: ${bindName}`)

              let elmItems = collectElmItems();
              if (elmItems.attribute != null) {
                elmItems.attribute(this.Cell, val);
                //                this.Cell[elmItems.attribute] = val;
                this.parent.dataLoadUpdaters.push([this.Cell, elmItems.event]);
              }
            };
            if (loadHoldingValue != null) binder.load(loadHoldingValue);

            // gets a value from UI element
            binder.get = (val) => {
              let elmItems = collectElmItems();
              if (elmItems.attribute != null) return null;
              return this.Cell[elmItems.attribute];
            };

            let getSource = () => {
              return childConfig.boundName
            }

            //            this.Cell.addEventListener(event, (evt) => {
            //              // this._data = creator.Cell[attribute];
            //              console.log(this.Cell);
            //            });
          };

          this.initCellBinding = () => runCellBinding();
          if (this.Cell != null)
            runBinding();
        }
      };
      creator.parent = this;
      if (childConfig?.boundName != null)
        creator.bindData();
      return creator;
    };

    ElemIDConfigs.forEach(elmConfig => {
      let outObj = makeDataObjs(elmConfig.root);
      elmConfig.children.forEach(cfg => {
        outObj[cfg.elmName] = makeDataObjs(elmConfig.root, cfg);
      })
      outData['cellIDs'][elmConfig.root] = outObj;
    });

    return outData;
  }

  runDataLoadUpdate() {
    this.dataLoadUpdaters.forEach(updateItem => {
      let elem = updateItem[0];
      let action = updateItem[1];
      // console.log(`Running action (${action}) on ${elem.id}`);
      elem.dispatchEvent(new Event(action));
    })
  }

  createTitleTable() {
    let itemTable = document.createElement('table');
    itemTable.className = 'sjs_group-item nested-2';
    itemTable.style.padding = '0px';
    itemTable.style.display = 'table';
    itemTable.style.tableLayout = 'fixed';

    let itemHead = document.createElement('thead');
    let itemHeadRow = document.createElement('tr');
    itemHeadRow.style.visibility = 'collapse';
    itemHead.appendChild(itemHeadRow);

    let headTH1 = document.createElement('th');
    headTH1.style.width = '1em';
    itemHeadRow.appendChild(headTH1);

    itemHeadRow.appendChild(document.createElement('th'));

    let headTH2 = document.createElement('th');
    headTH2.style.width = '2.5em';
    itemHeadRow.appendChild(headTH2);

    itemTable.appendChild(itemHead)

    let tableBody = itemTable.appendChild(document.createElement('tbody'));
    tableBody.appendChild(this.createTitleRow());

    return itemTable;
  }

  // "::: {Sequence text}
  createTitleRow() {
    let titleRow = document.createElement('tr');

    let titleHandleCell = document.createElement('td');
    this.TitleHandle = titleHandleCell;
    titleHandleCell.style.backgroundColor = 'unset';
    let titleHandle = document.createElement('div');
    titleHandle.className = 'sjs_handle';
    titleHandle.innerText = ':::';
    titleHandleCell.appendChild(titleHandle);
    titleRow.appendChild(titleHandleCell);

    // cell for View and Edit titles
    let titleTextCell = document.createElement('td');
    titleTextCell.style.backgroundColor = 'unset';
    titleTextCell.style.overflow = 'hidden';
    titleTextCell.style.textOverflow = 'ellipsis';
    titleRow.appendChild(titleTextCell);

    let titleView = document.createElement('div');
    this.cellIDs.viewTx.Title.setCell(titleView);
    this.titleView = titleView;
    titleView.style.fontFamily = "monospace";
    titleView.style.fontSize = "1.2em";
    titleTextCell.appendChild(titleView);

    let titleEdit = document.createElement('div');
    this.cellIDs.editTx.Title.setCell(titleEdit);
    this.titleEdit = titleEdit;
    titleEdit.style.fontFamily = "monospace";
    titleEdit.style.fontSize = "1.2em";
    titleEdit.style.display = 'none';
    titleEdit.style.color = 'white';
    titleTextCell.appendChild(titleEdit);

    // buttons, created separately
    titleRow.appendChild(this.createTitleRowBtns());

    return titleRow;
  }

  // (edit) / (delete, save, cancel)
  createTitleRowBtns() {
    var self = this;
    let buttonsTD = document.createElement('td');
    buttonsTD.style.backgroundColor = 'unset';
    buttonsTD.style.position = 'absolute';
    buttonsTD.style.right = '0';

    let btn = document.createElement('button');
    btn.type = 'button';
    this.cellIDs.root.Delete.setCell(btn);
    btn.style.display = 'none';
    btn.style.marginRight = '1.5em';
    btn.onclick = function() {
      let cfmMsg = 'This will delete the Sequence command.\n\n!!! The process is NOT reversible via mimik !!!';
      if (confirm(cfmMsg)) {
        let thisParent = self.thisView.parentElement;
        self.thisView.remove();
        flipperReadyDiv(thisParent);
      }
    };
    btn.innerText = 'Delete';
    buttonsTD.appendChild(btn);

    btn = document.createElement('button');
    btn.type = 'button';
    btn.innerText = 'Save';
    this.cellIDs.root.Save.setCell(btn);
    btn.style.display = 'none';
    btn.onclick = function() {
      self.titleView.innerText = self.titleEdit.innerText;
      toggleEditButton(self.itemID);
      self.itemData.Update(self.editData);
      document.p4ExportData.UpdateItemData(this.itemID, self.itemData.parserObj);
    };
    buttonsTD.appendChild(btn);

    btn = document.createElement('button');
    this.EditingBtn = btn;
    btn.type = 'button';
    btn.innerText = 'Edit';
    this.cellIDs.root.EditCancel.setCell(btn);
    btn.onclick = function() {
      if (btn.innerText == 'Edit') {
        self.TitleHandle.style.visibility = 'hidden';
        self.editData = self.itemData.parserObj.Clone();
      } else {
        self.TitleHandle.style.visibility = 'unset';
      }

      toggleEditButton(self.itemID);
      //      self.titleEdit.innerText = self.titleView.innerText;
    };
    buttonsTD.appendChild(btn);

    return buttonsTD;
  }

  enableEditing() {
    if (this.EditingBtn.innerText == 'Edit')
      this.EditingBtn.click();
  }

  // body element for sequence editing controls
  createCmdEditor() {
    let cmdEditorDiv = document.createElement('div');
    this.cellIDs.root.Content.setCell(cmdEditorDiv);
    cmdEditorDiv.style.display = 'none';

    let cmdTable = cmdEditorDiv.appendChild(document.createElement('table'));
    //    cmdTable.style.width = '100%';
    cmdTable.appendChild(this.cmdHeader());
    cmdTable.appendChild(this.cmdBody());

    return cmdEditorDiv;
  }

  // "Conditional", "Source", "Action" headers
  cmdHeader() {
    var self = this;
    let cmdTableHead = document.createElement('thead');
    let cmdHeadRow = cmdTableHead.appendChild(document.createElement('tr'));

    // Conditional header
    let cmdCondHead = cmdHeadRow.appendChild(document.createElement('th'));
    //    cmdCondHead.style.width = '18%';
    cmdCondHead.appendChild(tooltipText(
      'Conditional',
      'Determines pre/ post actions of a source\'s results'
    ));

    let cmdCondChkBox = cmdCondHead.appendChild(document.createElement('input'));
    cmdCondChkBox.type = 'checkbox';
    cmdCondChkBox.onclick = function() {
      self.toggleCondField(this);
      if (self.editData == undefined) return;
      self.editData.isCond = this.checked;
      self.UpdateEditView();
    };
    this.cellIDs.cond.Enabled.setCell(cmdCondChkBox);

    // Source header
    let cmdSourceHead = cmdHeadRow.appendChild(document.createElement('th'));
    cmdSourceHead.appendChild(tooltipText(
      'Source',
      'What is the context of this step'
    ));

    // Action header
    let cmdActionHead = cmdHeadRow.appendChild(document.createElement('th'));
    //    cmdActionHead.style.width = '18%';
    cmdActionHead.appendChild(tooltipText(
      'Action',
      'Processing with the source content'
    ));

    let cmdActionChkBox = cmdActionHead.appendChild(document.createElement('input'));
    cmdActionChkBox.type = 'checkbox';
    cmdActionChkBox.onclick = function() {
      self.toggleActionField(this);
      self.UpdateEditView();
    };
    this.cellIDs.act.Enabled.setCell(cmdActionChkBox);

    return cmdTableHead;
  }

  // toggle show/ hide of the Conditional cell states
  toggleCondField(chkBox) {
    if (chkBox.checked) {
      this.condFieldID_On.style.display = 'inline';
      this.condFieldID_Off.style.display = 'none';
    } else {
      this.condFieldID_On.style.display = 'none';
      this.condFieldID_Off.style.display = 'inline';
    }
  }

  // toggle show/ hide of the Action cell states
  toggleActionField(chkBox) {
    let actCell = this.cellIDs.act;
    let onCell = actCell.On.Cell.style;
    let offCell = actCell.Off.Cell.style;

    if (chkBox.checked) {
      onCell.display = 'inline';
      offCell.display = 'none';
    } else {
      onCell.display = 'none';
      offCell.display = 'inline';
    }
  }

  // command body; where the controls for changing the Command data are
  cmdBody() {
    let cmdTBody = document.createElement('tbody');
    let cmdRow = cmdTBody.appendChild(document.createElement('tr'));

    cmdRow.appendChild(this.condCell());
    cmdRow.appendChild(this.sourceCell());
    cmdRow.appendChild(this.actionCell());

    return cmdTBody;
  }

  // Cell containing the controls for "Conditional"
  condCell() {
    var self = this;
    let cell = document.createElement('td');

    let itemOffCell = cell.appendChild(document.createElement('div'));
    this.condFieldID_Off = itemOffCell;
    this.cellIDs.cond.Off.setCell(itemOffCell);
    itemOffCell.appendChild(tooltipText(
      'Disabled',
      'This step\'s end state will not affect other steps'
    ));

    let itemOnCell = cell.appendChild(document.createElement('div'));
    this.condFieldID_On = itemOnCell;
    this.cellIDs.cond.On.setCell(itemOffCell);
    itemOnCell.style.display = 'none';

    // Optional section
    itemOnCell.appendChild(tooltipText(
      'Optional',
      `True; further steps will still run even if this one doesn't pass.
       False: The conditional must pass for this and future lines to run.
      `
    ));
    let condOptionalChkBox = itemOnCell.appendChild(document.createElement('input'));
    condOptionalChkBox.style.marginLeft = '0.2em';
    condOptionalChkBox.type = 'checkbox';
    this.cellIDs.cond.Opt.setCell(condOptionalChkBox);
    condOptionalChkBox.onclick = function() {
      if (self.editData == undefined) return;
      self.editData.isOpt = this.checked;
      self.UpdateEditView();
    };

    itemOnCell.appendChild(document.createElement('br'))
    itemOnCell.appendChild(document.createElement('br'))

    // Requirement section
    itemOnCell.appendChild(tooltipText(
      'Requirement',
      'What kind of state must the source have to Pass'
    ));

    // Requirement Options
    let condReqOptions = itemOnCell.appendChild(document.createElement('select'));
    condReqOptions.style.marginLeft = '0.2em';
    appendOption(condReqOptions, 'None', true);
    appendOption(condReqOptions, 'True');
    appendOption(condReqOptions, 'False');
    condReqOptions.onchange = function() {
      if (self.editData == undefined) return;
      self.editData.condSrc = this.selectedIndex;
      self.UpdateEditView();
    };
    this.cellIDs.cond.ReqT.setCell(condReqOptions);

    return cell;
  }

  // Cell containing the controls for "Source"
  sourceCell() {
    let cell = document.createElement('td');

    let cellTable = cell.appendChild(document.createElement('table'));
    cellTable.style.border = '0';

    // Column setup
    let cellTableHead = cellTable.appendChild(document.createElement('thead'));
    let cTableHeadRow = cellTableHead.appendChild(document.createElement('tr'));
    let ctHead1 = cTableHeadRow.appendChild(document.createElement('td'));
    //    ctHead1.style.width = '30%';
    ctHead1.style.padding = '0';
    let ctHead2 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead2.style.padding = '0';

    // Add data row to the table
    this.sourceCellData(cellTable);

    return cell;
  }

  // Cell containing the controls for "Action"
  actionCell() {
    var self = this;
    let cell = document.createElement('td');

    let itemOffCell = cell.appendChild(document.createElement('div'));
    this.cellIDs.act.Off.setCell(itemOffCell);
    itemOffCell.appendChild(tooltipText(
      'Disabled',
      'This command will have no post actions'
    ));

    let itemOnCell = cell.appendChild(document.createElement('div'));
    this.cellIDs.act.On.setCell(itemOnCell);
    itemOnCell.style.display = 'none';
    this.actionCellData(itemOnCell);

    return cell;
  }

  // Setup of the Source Cell rows (appending to cellTable)
  sourceCellData(cellTable) {
    let tableBody = cellTable.appendChild(document.createElement('tbody'));
    tableBody.appendChild(this.data_SourceTypeRow());
    tableBody.appendChild(this.data_htmlSubTypeRow());
    tableBody.appendChild(this.data_VarScopeCell());
    tableBody.appendChild(this.data_VarSearchCell());
    tableBody.appendChild(this.data_sourceIndex());
    tableBody.appendChild(this.data_sourceMatch());
  }

  /*
   Row for: Source's item type
   - None, Request, Response, Variable, Uses
  */
  data_SourceTypeRow() {
    var self = this;
    let row = document.createElement('tr');

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Root source type',
      'What data this command will interact with'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataSelect = cellData.appendChild(document.createElement('select'));
    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'Request');
    appendOption(dataSelect, 'Response');
    appendOption(dataSelect, 'Variable');
    appendOption(dataSelect, 'Uses');
    dataSelect.onchange = function() {
      let elms = self.cellIDs;
      let RTypeCell = self.cellIDs.srcRType.Row.Cell.style;
      let VScpCell = self.cellIDs.srcVarScope.Row.Cell.style;
      let VSrcCell = self.cellIDs.srcVarSrcUp.Row.Cell.style;
      let srcIName = self.cellIDs.srcIName.Row.Cell.style;
      let srcIMatch = self.cellIDs.srcIMatch.Row.Cell.style;

      let srcINameData = self.cellIDs.srcIName.Data.Cell;
      let srcIMatchData = self.cellIDs.srcIMatch.Data.Cell;

      switch (this.selectedIndex) {
        case 0:
          RTypeCell.display = 'none';
          VScpCell.display = 'none';
          VSrcCell.display = 'none';
          srcIName.display = 'none';
          srcIMatch.display = 'none';
          break;
        case 1: // request
        case 2: // response
          VScpCell.display = 'none';
          VSrcCell.display = 'none';
          RTypeCell.display = '';
          break;
        case 3: // variable
          srcINameData.placeholder = 'Variable key';
          srcIMatchData.placeholder = 'Variable matching';
          RTypeCell.display = 'none';
          VScpCell.display = '';
          VSrcCell.display = '';
          srcIName.display = '';
          srcIMatch.display = '';
          break;
        case 4: // use
          srcINameData.placeholder = "Other chapter's name";
          srcIMatchData.placeholder = 'Use value equation';
          RTypeCell.display = 'none';
          VScpCell.display = 'none';
          VSrcCell.display = 'none';
          srcIName.display = '';
          srcIMatch.display = '';
          break;
      }

      if (self.editData == undefined) return;
      self.editData.srcType = this.selectedIndex;
      self.UpdateEditView();
    };

    this.cellIDs.src.Type.setCell(dataSelect);

    return row;
  }

  // Row for: Source (Request/ Response)'s sub type
  // - None, Head, Body
  data_htmlSubTypeRow() {
    var self = this;
    let row = document.createElement('tr');
    this.cellIDs.srcRType.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'HTML sub-source type',
      'What data this command will interact with'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataSelect = cellData.appendChild(document.createElement('select'));
    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'Head');
    appendOption(dataSelect, 'Body');
    dataSelect.onchange = function() {
      let iName = self.cellIDs.srcIName.Row.Cell;
      let iMatch = self.cellIDs.srcIMatch.Row.Cell;
      let iNameData = self.cellIDs.srcIName.Data.Cell;
      let iMatchData = self.cellIDs.srcIMatch.Data.Cell;

      switch (this.selectedIndex) {
        case 1:
          iNameData.placeholder = 'Header key';
          iMatchData.placeholder = 'Header matching';
          iName.style.display = '';
          iMatch.style.display = '';
          break;
        case 2:
          iNameData
          iMatchData.placeholder = 'Body matching';
          iName.style.display = 'none';
          iMatch.style.display = '';
          break;
      }

      if (self.editData == undefined) return;
      self.editData.isHead = this.selectedIndex == 1;
      self.editData.isBody = this.selectedIndex == 2;
      if (row.style.display == '')
        self.UpdateEditView();
    };

    this.cellIDs.srcRType.Data.setCell(dataSelect);

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcRType.Data.Cell.selectedIndex = 0;
        self.cellIDs.srcRType.Data.Cell.onchange();
      }
    }).observe(row, {
      attributes: true,
      attributeFilter: ['style']
    });
    return row;
  }

  /*
   Scope of the variable source
   - Self, Chapter, Test Bounds
  */
  data_VarScopeCell() {
    var self = this;
    let row = document.createElement('tr');
    this.cellIDs.srcVarScope.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Variable scope level',
      'Scope area of the search'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataSelect = cellData.appendChild(document.createElement('select'));
    appendOption(dataSelect, '0: Self', true);
    appendOption(dataSelect, '1: Chapter');
    appendOption(dataSelect, '2: Test Bounds');
    dataSelect.onchange = function() {
      if (self.editData == undefined) return;
      self.editData.varLevel = this.selectedIndex;
      self.UpdateEditView();
    };
    this.cellIDs.srcVarScope.Data.setCell(dataSelect);

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcVarScope.Data.Cell.selectedIndex = 0;
        self.cellIDs.srcVarScope.Data.Cell.onchange();
      }
    }).observe(row, {
      attributes: true,
      attributeFilter: ['style']
    });

    return row;
  }

  /*
   Determines if the scope is allowed to use it's parents
   - True/ False
  */
  data_VarSearchCell() {
    var self = this;
    let row = document.createElement('tr');
    this.cellIDs.srcVarSrcUp.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Variable hierarchy search',
      'When set, parent variables will also be searched'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataBox = cellData.appendChild(document.createElement('input'));
    dataBox.type = 'checkbox';
    this.cellIDs.srcVarSrcUp.Data.setCell(dataBox);
    dataBox.onclick = function() {
      self.editData.varSearchUp = this.checked;
      self.UpdateEditView();
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcVarSrcUp.Data.Cell.checked = false;
        self.cellIDs.srcVarSrcUp.Data.Cell.onclick();
      }
    }).observe(row, {
      attributes: true,
      attributeFilter: ['style']
    });

    return row;
  }

  /*
   Index for the source item
   Valid types: rType's head, vType, uType
   - enable/ disabled
   - Index text
  */
  data_sourceIndex() {
    var self = this;
    let row = document.createElement('tr');
    this.cellIDs.srcIName.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    let cellHeadChkBox = cellHead.appendChild(document.createElement('input'));
    cellHeadChkBox.type = 'checkbox';
    this.cellIDs.srcIName.State.setCell(cellHeadChkBox);
    cellHeadChkBox.style.marginRight = '1em';
    cellHeadChkBox.onclick = function() {
      let srINameObj = self.cellIDs.srcIName;
      if (this.checked) {
        srINameObj.Off.Cell.style.display = 'none';
        srINameObj.On.Cell.style.display = '';
      } else {
        srINameObj.Data.Cell.value = '';
        srINameObj.Off.Cell.style.display = '';
        srINameObj.On.Cell.style.display = 'none';
        if (self.editData != undefined)
          self.editData.source_name = '';
      }

      if (row.style.display == '')
        self.UpdateEditView();
    };

    cellHead.appendChild(tooltipText(
      'Source index',
      'Item Index within the source item.'
    ));

    let cellDataOff = row.appendChild(document.createElement('td'));
    this.cellIDs.srcIName.Off.setCell(cellDataOff);
    cellDataOff.appendChild(tooltipText(
      'Disabled',
      'No items within the source will be referenced.'
    ));

    let cellDataOn = row.appendChild(document.createElement('td'));
    this.cellIDs.srcIName.On.setCell(cellDataOn);
    cellDataOn.style.display = 'none';

    let dataOnInput = cellDataOn.appendChild(document.createElement('input'));
    this.cellIDs.srcIName.Data.setCell(dataOnInput);
    dataOnInput.style.width = '100%';
    dataOnInput.placeholder = 'Reference Item'
    dataOnInput.oninput = function(evt) {
      if (self.editData == undefined) return;
      self.editData.source_name = evt.target.value;
      self.UpdateEditView();
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcIName.State.Cell.checked = false;
        self.cellIDs.srcIName.State.Cell.onclick();
        self.UpdateEditView();
      }
    }).observe(row, {
      attributes: true,
      attributeFilter: ['style']
    });

    return row;
  }

  /*
   Matching section for source item
   Valid types: all
   - enable/ disable
   - matcher string
  */
  data_sourceMatch() {
    var self = this;
    let row = document.createElement('tr');
    this.cellIDs.srcIMatch.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    let cellHeadChkBox = cellHead.appendChild(document.createElement('input'));
    cellHeadChkBox.type = 'checkbox';
    this.cellIDs.srcIMatch.State.setCell(cellHeadChkBox);
    cellHeadChkBox.style.marginRight = '1em';
    cellHeadChkBox.onclick = function() {
      let srIMatchObj = self.cellIDs.srcIMatch;
      if (this.checked) {
        srIMatchObj.Off.Cell.style.display = 'none';
        srIMatchObj.On.Cell.style.display = '';
      } else {
        srIMatchObj.Data.Cell.value = '';
        srIMatchObj.Off.Cell.style.display = '';
        srIMatchObj.On.Cell.style.display = 'none';
        if (self.editData != undefined)
          self.editData.source_match = '';
      }

      if (row.style.display == '')
        self.UpdateEditView();
    };

    cellHead.appendChild(tooltipText(
      'Source match',
      'Item matching within the source item.'
    ));

    let cellDataOff = row.appendChild(document.createElement('td'));
    this.cellIDs.srcIMatch.Off.setCell(cellDataOff);
    cellDataOff.appendChild(tooltipText(
      'Disabled',
      'No item matching within the source will be made.'
    ));

    let cellDataOn = row.appendChild(document.createElement('td'));
    this.cellIDs.srcIMatch.On.setCell(cellDataOn);
    cellDataOn.style.display = 'none';

    let dataOnInput = cellDataOn.appendChild(document.createElement('input'));
    this.cellIDs.srcIMatch.Data.setCell(dataOnInput);
    dataOnInput.style.width = '100%';
    dataOnInput.placeholder = 'Reference Item'
    dataOnInput.oninput = function(evt) {
      if (self.editData == undefined) return;
      self.editData.source_match = evt.target.value;
      self.UpdateEditView();
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcIMatch.State.Cell.checked = false;
        self.cellIDs.srcIMatch.State.Cell.onclick();
      }
    }).observe(row, {
      attributes: true,
      attributeFilter: ['style']
    });

    return row;
  }

  /*
   Body for Command's action items
   - Type: Variable or match
   - Variable fields
   - Matcher Fields
  */
  actionCellData(cellTable) {
    var self = this;

    cellTable.appendChild(this.actionType());
    cellTable.appendChild(this.actionVarCell());
    cellTable.appendChild(this.actionMatchCell());

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.editData.act_match = null;
        self.editData.act_name = null;
        self.cellIDs.actVar.Row.Cell.style.display = 'none';
        self.cellIDs.actMatch.Row.Cell.style.display = 'none';
        let typeOptionID = self.cellIDs.act.typeOption.ID;
        document.querySelector(`input[name=${typeOptionID}]:checked`).checked = false;
        document.querySelector(`input[id='${typeOptionID}_default']`).checked = true;
        self.UpdateEditView();
      }
    }).observe(cellTable, {
      attributes: true,
      attributeFilter: ['style']
    });
  }

  /*
   Cell which determines the type of action
   - Variable
   - Matcher
  */
  actionType() {
    var self = this;

    let typeTable = document.createElement('table');

    let cellTableHead = typeTable.appendChild(document.createElement('thead'));
    let cTableHeadRow = cellTableHead.appendChild(document.createElement('tr'));
    let ctHead1 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead1.style.width = '50%';
    ctHead1.style.padding = '0';
    let ctHead2 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead2.style.width = '50%';
    ctHead2.style.padding = '0';

    let tableBody = typeTable.appendChild(document.createElement('tbody'));

    let infoRow = tableBody.appendChild(document.createElement('tr'));
    let infoCell = infoRow.appendChild(document.createElement('td'));
    infoCell.colSpan = "2";
    infoCell.style.textAlign = 'center';
    infoCell.appendChild(tooltipText(
      'Action type',
      'What this command will do with data', 'left'
    ));

    let optionRow = tableBody.appendChild(document.createElement('tr'));

    function onChangeFunction() {
      let ActMatchRow = self.cellIDs.actMatch.Row.Cell.style;
      let ActVarRow = self.cellIDs.actVar.Row.Cell.style;
      let editObj = self.editData?.parserObj;
      if (option1Data.checked) {
        ActVarRow.display = 'none';
        ActMatchRow.display = '';
        if (editObj != undefined) {
            editObj.act_name = null;
            editObj.act_match = '';
            self.UpdateEditView();
        }
      } else if (option2Data.checked) {
        ActVarRow.display = '';
        ActMatchRow.display = 'none';
        if (editObj != undefined) {
          editObj.act_name = '';
          editObj.act_match = null;
          self.UpdateEditView();
        }
      } else {
        console.info("testing 123")
      }
    };

    function createRadioOption(rName) {
      let rOption = document.createElement('input');
      rOption.type = 'radio';
      rOption.name = rName;
      rOption.onchange = () => onChangeFunction();
      return rOption;
    }

    var optGroupName = this.cellIDs.act.typeOption.ID;
    let option1Cell = optionRow.appendChild(document.createElement('td'));
    let op1CellStyle = option1Cell.style;
    op1CellStyle.textAlign = 'center';
    op1CellStyle.borderRightWidth = '1px';
    op1CellStyle.borderRightStyle = 'solid';

    // hidden "nothing selected" item
    let option0Data = option1Cell.appendChild(createRadioOption(optGroupName));
    option0Data.id = optGroupName + '_default';
    option0Data.style.display = 'none';
    option0Data.checked = true;

    // first visible option - "To Source"
    let info1Div = option1Cell.appendChild(tooltipText(
      'To Source',
      'What this command will do with data', 'left'
    ));
    info1Div.style.textAlign = 'center';
    let option1Data = option1Cell.appendChild(createRadioOption(optGroupName));
    this.cellIDs.act.TypeSrc.setCell(option1Data);

    // second visible option - "To Variable"
    let option2Cell = optionRow.appendChild(document.createElement('td'));
    option2Cell.style.textAlign = 'center';
    let info2Div = option2Cell.appendChild(tooltipText(
      'To Variable',
      'Where the command will put the data', 'left'
    ));
    info2Div.style.textAlign = 'center';
    let option2Data = option2Cell.appendChild(createRadioOption(optGroupName));
    this.cellIDs.act.TypeVar.setCell(option2Data);

    return typeTable
  }

  /*
    Body content for matcher actions
    - input
   */
  actionMatchCell() {
    let tableCell = document.createElement('table');
    tableCell.style.display = 'none';
    this.cellIDs.actMatch.Row.setCell(tableCell);
    let cellTableHead = tableCell.appendChild(document.createElement('thead'));
    let cTableHeadRow = cellTableHead.appendChild(document.createElement('tr'));
    let ctHead1 = cTableHeadRow.appendChild(document.createElement('td'));
    //    ctHead1.style.width = '30%';
    ctHead1.style.padding = '0';
    let ctHead2 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead2.style.padding = '0';

    let cellTableBody = tableCell.appendChild(document.createElement('tbody'));
    let cellRow = cellTableBody.appendChild(document.createElement('tr'));

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Result',
      'Resulting data will be applied to the source\'s content', 'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataOnInput = cellBody.appendChild(document.createElement('input'));
    this.cellIDs.actMatch.Data.setCell(dataOnInput);
    dataOnInput.style.width = '100%';
    dataOnInput.placeholder = 'Data action'
    dataOnInput.oninput = function(evt) {
      if (self.editData == undefined) return;
      self.editData.act_match = evt.target.value;
      self.UpdateEditView();
    };

    var self = this;
    respondToVisibility(tableCell, visible => {
      if (self.editData == undefined) return;
      let actState = self.cellIDs.act.Enabled.Cell.checked;
      let typeID = self.cellIDs.act.TypeSrc.ID;
      let typeState = document.querySelector(`input[id='${typeID}']`).checked;

      if (!visible && (!actState || !typeState)) {
        self.cellIDs.actMatch.Data.Cell.value = '';
        self.editData.act_match = '';
      }
    });

    let cellInfoRow = cellTableBody.appendChild(document.createElement('tr'));
    let cellInfo = cellInfoRow.appendChild(document.createElement('td'));
    let info1Help = cellInfo.appendChild(tooltipText(
      'Using variables',
      `1. Wrap variable within "@{}"
      - Example 1: "...->{value is @{sizeValue} px}"
      - Example 2: "...->{username: @{%uName} px}"

      2. Variable usage
      - source match (at group 1): "...->{@{1}}"
      - source match (at group "name"): "...->{@{name}}"
      - local: "...->{@{localVar}}"
      - chapter (&): "...->{@{&chapVar}}"
      - test bound (%): "...->{@{%boundVar}}"

      3. Alternate usages:
      - example: "...->{using @{varA|varB}}"
      - note: value is skipped when uninitialized or empty length

      4. Using fallback values: "...->{@{...|'fallback'}}"
      - Example: "...->{Value = @{varA|'no data'}}"
      `, 'left'
    ));
    info1Help.firstElementChild.style.textAlign = "left";
    info1Help.firstElementChild.style.fontFamily = "monospace";

    return tableCell;
  }

  actionVarCell() {
    // ['actVar', ['Row', 'Scope', 'Data']],
    /*
    row: this table
    scope: actionVarScopeRow
    data: actionVarNameRow
    exists:
    */
    let tableCell = document.createElement('table');
    this.cellIDs.actVar.Row.setCell(tableCell);
    tableCell.style.display = 'none';
    let cellTableHead = tableCell.appendChild(document.createElement('thead'));
    let cTableHeadRow = cellTableHead.appendChild(document.createElement('tr'));
    let ctHead1 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead1.style.padding = '0';
    let ctHead2 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead2.style.padding = '0';

    let cellTableBody = tableCell.appendChild(document.createElement('tbody'));
    cellTableBody.appendChild(this.actVarNameRow());
    cellTableBody.appendChild(this.actVarNameScopeRow());
    cellTableBody.appendChild(this.actPostVarActionsRow());

    var self = this;
    respondToVisibility(tableCell, visible => {
      if (self.editData == undefined) return;
      let actState = self.cellIDs.act.Enabled.Cell.checked;
      let typeID = self.cellIDs.act.TypeVar.ID;
      let typeState = document.querySelector(`input[id='${typeID}']`).checked;

      if (!visible && (!actState || !typeState)) {
        self.cellIDs.actVar.Data.Cell.value = '';
        self.cellIDs.actVar.Scope.Cell.selectedIndex = 0;
        self.cellIDs.actVarPost.Exists.Cell.checked = false;
        self.cellIDs.actVarPost.Count.Cell.checked = false;
        self.cellIDs.actVarPost.Result.Cell.checked = false;
        self.cellIDs.actVarPost.Spread.Cell.selectedIndex = 0;
        self.cellIDs.actVarPost.IndexRow.Cell.style.display = 'none';

        let editObj = self.editData;
        editObj.act_name = '';
        editObj.act_scopeLevel = 0;
        editObj.act_nExists = false;
        editObj.act_nCount = false;
        editObj.act_nResult = false;
        editObj.act_nSpreadType = -8;
      }
    });

    return tableCell;
  }

  // Row where the user can enter the variable's name
  actVarNameRow() {
    var self = this;
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Variable name',
      'Resulting variable name', 'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataOnInput = cellBody.appendChild(document.createElement('input'));
    this.cellIDs.actVar.Data.setCell(dataOnInput);
    dataOnInput.style.width = '100%';
    dataOnInput.placeholder = 'Name'
    dataOnInput.oninput = function(evt) {
      if (self.editData == undefined) return;
      self.editData.act_name = evt.target.value;
      self.UpdateEditView();
    };

    return cellRow;
  }

  actVarNameScopeRow() {
    var self = this;
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Save location',
      'Which scope level to save the variable to', 'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataSelect = cellBody.appendChild(document.createElement('select'));
    appendOption(dataSelect, '0: Self', true);
    appendOption(dataSelect, '1: Chapter');
    appendOption(dataSelect, '2: Test Bounds');
    dataSelect.onchange = function() {
      if (self.editData == undefined) return;
      self.editData.act_scopeLevel = this.selectedIndex;
      self.UpdateEditView();
    };
    this.cellIDs.actVar.Scope.setCell(dataSelect);
    return cellRow;
  }

  actPostVarActionsRow() {
    //['actVarPost', ['Row', 'Exists', 'Count', 'Result', 'Spread']],
    let cellRow = document.createElement('tr');
    this.cellIDs.actVarPost.Row.setCell(cellRow);

    let cellContent = cellRow.appendChild(document.createElement('td'));
    cellContent.colSpan = 2;
    let tableCell = cellContent.appendChild(document.createElement('table'));
    let cellTableHead = tableCell.appendChild(document.createElement('thead'));
    let cTableHeadRow = cellTableHead.appendChild(document.createElement('tr'));
    let ctHead1 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead1.style.padding = '0';
    let ctHead2 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead2.style.padding = '0';

    let cellTableBody = tableCell.appendChild(document.createElement('tbody'));
    cellTableBody.appendChild(this.actPostFlagInfo());
    cellTableBody.appendChild(this.actPostExists());
    cellTableBody.appendChild(this.actPostCount());
    cellTableBody.appendChild(this.actPostResults());
    cellTableBody.appendChild(this.actPostSpread());
    cellTableBody.appendChild(this.actPostSpreadIndex());

    return cellRow;
  }

  // highlight text about what this table is about
  actPostFlagInfo() {
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.colSpan = 2;
    cellHead.style.textAlign = 'center';
    cellHead.appendChild(tooltipText(
      'Additional variables creation',
      'Enabling the following flags will create additional variables along with the above variable.',
      'left'
    ));

    return cellRow;
  }

  // Row for enabling var flag: source exists
  actPostExists() {
    var self = this;
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Exists',
      'Results of the Source state will be saved to an additional variable.\n' +
      'Example: name_? => true',
      'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataBox = cellBody.appendChild(document.createElement('input'));
    dataBox.type = 'checkbox';
    this.cellIDs.actVarPost.Exists.setCell(dataBox);
    dataBox.onclick = function() {
      if (self.editData == undefined) return;
      self.editData.act_nExists = this.checked;
      self.UpdateEditView();
    };

    return cellRow;
  }

  // Row for enabling var flag: match count
  actPostCount() {
    var self = this;
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Count',
      'How many results were found from the source match.\n' +
      'Ex: name_# => 12',
      'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataBox = cellBody.appendChild(document.createElement('input'));
    dataBox.type = 'checkbox';
    this.cellIDs.actVarPost.Count.setCell(dataBox);
    dataBox.onclick = function() {
      if (self.editData == undefined) return;
      self.editData.act_nCount = this.checked;
      self.UpdateEditView();
    };

    return cellRow;
  }

  // Row for enabling var flag: source result state
  actPostResults() {
    var self = this;
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Result',
      'Source\'s conditional result.\n' +
      'Ex: name_@ => true', 'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataBox = cellBody.appendChild(document.createElement('input'));
    dataBox.type = 'checkbox';
    this.cellIDs.actVarPost.Result.setCell(dataBox);
    dataBox.onclick = function() {
      if (self.editData == undefined) return;
      self.editData.act_nResult = this.checked;
      self.UpdateEditView();
    };

    return cellRow;
  }

  // Row for enabling var flag spread types
  actPostSpread() {
    var self = this;
    let cellRow = document.createElement('tr');

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Spread',
      'Export all the matched results as their own variable.\n' +
      'Ex: name_0, name_1, name_3, etc.',
      'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataSelect = cellBody.appendChild(document.createElement('select'));
    dataSelect.style.width = '50%';
    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'All');
    appendOption(dataSelect, 'Last');
    appendOption(dataSelect, 'Single index');
    dataSelect.onchange = function() {
      let indexSpreadCell = self.cellIDs.actVarPost.IndexRow.Cell.style;
      switch (this.selectedIndex) {
        case 0:
        case 1:
        case 2:
          indexSpreadCell.display = 'none';
          if (self.editData != undefined) {
            let editObj = self.editData;
            editObj.act_nSpreadType = -this.selectedIndex;
            if (editObj.act_nSpreadType == 0)
              editObj.act_nSpreadType = -8;
            self.UpdateEditView();
          }
          break;

        case 3:
          indexSpreadCell.display = '';
          break;
      }
    };

    this.cellIDs.actVarPost.Spread.setCell(dataSelect);

    return cellRow;
  }

  // Row for enabling var spread flag (single index)
  actPostSpreadIndex() {
    var self = this;
    let cellRow = document.createElement('tr');
    this.cellIDs.actVarPost.IndexRow.setCell(cellRow);
    cellRow.style.display = 'none';

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Index',
      'Export all the matched results as their own variable.\n' +
      'Ex:\n\t0 => name_0\n\t13 => name_13',
      'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataOnInput = cellBody.appendChild(document.createElement('input'));
    this.cellIDs.actVarPost.Index.setCell(dataOnInput);
    dataOnInput.style.width = '50%';
    dataOnInput.placeholder = 'Index (0 -> xxx)'
    dataOnInput.oninput = function(evt) {
      let inputStr = parseInt(evt.target.value);
      if (isNaN(inputStr))
        inputStr = null;
      dataOnInput.value = inputStr;

      if (cellRow.style.display === '') {
        if (self.editData == undefined) return;
        self.editData.act_nSpreadType = inputStr || -8;
        self.UpdateEditView();
      }
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcIMatch.State.Cell.checked = false;
        self.cellIDs.srcIMatch.State.Cell.onclick();
      }
    }).observe(cellRow, {
      attributes: true,
      attributeFilter: ['style']
    });

    return cellRow;
  }

}
