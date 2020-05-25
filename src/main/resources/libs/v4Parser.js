
function enableSortRoot() {
  Sortable.create(level_root, {
    group: 'level_root',
    handle: '.sjs_handle',
    ghostClass: 'sjs_ghost',
    filter: '.sjs_noDrag',
    preventOnFilter: false,
    animation: 100
  });
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
    titleView: getElem('viewTx_' + sID),
    titleEdit: getElem('editTx_' + sID),
    editCancelBtn: getElem('editCancelBtn_' + sID),
    saveBtn: getElem('saveBtn_' + sID),
    deleteBtn: getElem('deleteBtn_' + sID),
    editingView: getElem('editContent_' + sID)
  }
}

function toggleEditButton(sID) {
  let seqIDs = seqContentIDs(sID);
  if (seqIDs.editingView.style.display == 'none') {
    seqIDs.titleView.style.display = 'none';
    seqIDs.titleEdit.style.display = 'block';

    seqIDs.editCancelBtn.innerHTML = 'Cancel';
    seqIDs.saveBtn.style.display = 'inline';
    seqIDs.deleteBtn.style.display = 'inline';

    seqIDs.editingView.style.display = 'block';
  } else {
    seqIDs.titleView.style.display = 'block';
    seqIDs.titleEdit.style.display = 'none';

    seqIDs.editCancelBtn.innerHTML = 'Edit';
    seqIDs.saveBtn.style.display = 'none';
    seqIDs.deleteBtn.style.display = 'none';

    seqIDs.editingView.style.display = 'none';
  }
}

function enableSortLevel(level_id) {
  var options = {
    group: 'level_1',
    handle: '.sjs_handle',
    ghostClass: 'sjs_ghost',
    animation: 100,
    onUpdate: function(evt) {
    console.info('Update ITEM!!');
      var hostChildren = [...evt.from.children];
      var childIdx = 0;
      hostChildren.filter(t => t.hasAttribute('idCode'))
        .forEach(childDiv => {
          var childID = childDiv.attributes.idCode.value;
          data[childID].newIndex = childIdx;
          childIdx++;
        });
    },
    onAdd: function(evt) {
      var hostID = evt.from.attributes.idCode.value;
      var itemID = evt.item.attributes.idCode.value;
      console.info('Added ITEM!!');
      data[itemID].newParent = hostID;
    },
    onChange: function(evt) {
      flipperReadyDiv(evt.from);
      flipperReadyDiv(evt.to);
    },
    onRemove: function(evt) {
      console.log({
        'event': 'onRemove',
        'this': this,
        'item': evt.item,
        'from': evt.from,
        'to': evt.to,
        'oldIndex': evt.oldIndex,
        'newIndex': evt.newIndex
      });
    }
  }

  level_id.attributes['sortable'] = options;
  Sortable.create(level_id, options);
}

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

function appendOption(selectElm, text, select = false) {
  let opt = selectElm.appendChild(document.createElement('option'));
  opt.innerText = text;
  opt.selected = select;
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
    if(inData == null || typeof inData != 'object' || !inData.isValid)
        inData = {};

    this.isCond = inData.isCond || false;
    this.isOpt = inData.isOpt || false;
    this.condSrc = inData.condSrc || 0;

    this.srcType = inData.srcType || 0;
    this.isHead = inData.isHead || false;
    this.isBody = inData.isBody || false;
    this.varLevel = inData.varLevel || 0;

    this.source_hasItems = inData.source_hasItems || false;
    this.source_name = inData.source_name || null;
    this.source_match = inData.source_match || null;

    this.act_hasItem = inData.act_hasItem || false;
    this.act_name = inData.act_name || null;
    this.act_scopeLevel = inData.act_scopeLevel || false;
    this.act_match = inData.act_match || null;
  }

  Clone() {
    return JSON.parse(JSON.stringify(this));
  }

  get Result() {
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

      switch(this.srcType) {
        case 0:
          return 'No Data set';
          break;

        case 1:
          output += 'request:';
          if (this.isHead) {
            output += 'head';
          } else if (this.isBody) {
            output += 'body';
          }
          break;

        case 2:
          output += 'response:';
          if (this.isHead) {
            output += 'head';
          } else if (this.isBody) {
            output += 'body';
          }
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
          output += 'var';
          break;

        case 4:
          output += 'use';
          break;
      }

      if(!valid)
        return "Invalid";

      if (this.source_hasItems) {
        if (this.source_name != null) output += '[\'' + this.source_name + '\']';
        if (this.source_match != null) output += ':{' + this.source_match + '}';
      }

      if (this.act_hasItem) {
        output += '->';
        if (this.act_name != null) {
          switch (this.act_scopeLevel) {
            case 1:
              output += '&';
              break;
            case 2:
              output += '%';
              break;
          }
          output += this.act_name;
        } else if (this.act_match != null) {
          output += '{' + this.act_match + '}'
        }
      }

      return output;
    }

  shuffle() {
      this.isCond = randomBool();
      this.isOpt = randomBool();
      this.condSrc = randomInt(-1, 2);

      this.srcType = randomInt(0, 2);
      this.isRequest = randomBool();
      this.isResponse = randomBool();
      this.isHead = randomBool();
      this.isBody = randomBool();
      this.varLevel = randomInt(0, 2);

//      this.source_hasItems = randomBool();
      this.source_name = randomBool() ? randomChars() : null;
      this.source_match = randomBool() ? randomChars() : null;
      this.source_hasItems = (this.source_name != null || this.source_match != null);

//      this.act_hasItem = randomBool();
      this.act_name = randomBool() ? randomChars() : null;
      this.act_scopeLevel = randomInt(-1, 2);
      this.act_match = randomBool() ? randomChars() : null;
      this.act_hasItem = (this.act_name != null ||
            this.act_scopeLevel > -1 || this.act_match != null);

      return this.Result;
  }
}

class parserEditorIDS {
  constructor(dataID) {
    this.dataID = dataID;
  }

  get parser_condOff() { return 'parser_condOff_' + this.dataID; }
  get parser_condOn() { return 'parser_condOn_' + this.dataID; }

  get source_Type() { return 'srcType_' + this.dataID; }
  get source_sType() { return 'srcRType_' + this.dataID; }

  get parser_condOff() { return 'parser_confOff_' + this.dataID; }
}

// HostID (+ [{data}, {data}, ...]
class parserEditor {
  constructor(hostID) {
    this.hostID = hostID || uniqueRandom(rr => (data[rr] === undefined) ? rr : -1);
  }

  AddNewSeqList(inData) {
    var listRoot = document.getElementById('level_root');
    if (listRoot == null) return;

    //    data[this.dataID] = {
    //      parent: 'Level1_' + this.dataID,
    //      newParent: -1,
    //      newIndex: -1,
    //      data: new p4Parser(inData)
    //    };

    let mainTable = this.createMainTable();
    let mainBody = mainTable.querySelector('tbody')
    mainBody.appendChild(this.createHeader());
    mainBody.appendChild(this.createContent());

    let togBtn = getElementById(mainBody, 'level0ContentToggle_' + this.hostID)
    let togElm = getElementById(mainBody, 'level0ContentWrapper_' + this.hostID)
    enableToggleArea(togBtn, togElm);

    listRoot.appendChild(mainTable);
  }

  LoadSeqData(data) {
    data.forEach(command => {
        let commandContent = new parserCommand(this.hostID);
        this.hostDiv.appendChild(commandContent.CreateView());
    });
    flipperReadyDiv(this.hostDiv);
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

  createHeader() {
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
    btn.innerText = 'Sequence ' + this.hostID;
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
    readyDiv.innerText = 'Drag command items here to add';
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
      flipperReadyDiv(this.hostDiv);
    });
    newSeqDiv.appendChild(newSeqBtn);

    return newSeqDiv;
  }
}

class parserCommand {
  constructor(hostID, itemID) {
    this.hostID = hostID;
    this.itemID = itemID || uniqueRandom(rr => (data[rr] === undefined) ? rr : -1);
    this.itemData = {
      parent: this.hostID,
      newParent: -1,
      newIndex: -1,
      data: new p4Parser(),
      Clone: function() { return JSON.parse(JSON.stringify(this)) },
    };

    this.cmdData = this.itemData.data;
    data[this.itemID] = this.itemData;
  }

  CreateView() {
    this.cellIDs = this.sourceCellIDs();
    let itemDiv = document.createElement('div');
    this.thisView = itemDiv;
    itemDiv.setAttribute('idCode', this.itemID);
    border3D(itemDiv);
    itemDiv.style.marginTop = '4px';
    itemDiv.style.marginBottom = '4px';

    itemDiv.appendChild(this.createTitleTable());
    itemDiv.appendChild(this.createCmdEditor());

    return itemDiv;
  }

  SetupAsData(dataObj) {
    this.itemData.data = new p4Parser();
    this.cmdData = this.itemData.data;

    this.cellIDs.elems.cmdCondChkBox.checked = this.cmdData.isCond;
    this.cellIDs.elems.cmdCondChkBox.onclick();
    this.cellIDs.elems.condOptionalChkBox.checked = this.cmdData.isOpt;
    this.cellIDs.elems.condOptionalChkBox.onclick();

    this.cellIDs.elems.condReqOptions.selectedIndex = Math.max(0, this.cmdData.condSrc);

    this.cellIDs.elems.rootType.selectedIndex = this.cmdData.srcType;
    this.cellIDs.elems.rootType.onchange();

    if(this.cmdData.isHead)
      this.cellIDs.elems.source_RSub.selectedIndex = 1;
    else if(this.cmdData.isBody)
      this.cellIDs.elems.source_RSub.selectedIndex = 2;
    else
      this.cellIDs.elems.source_RSub.selectedIndex = 0;
    this.cellIDs.elems.source_RSub.onchange();

    this.cellIDs.elems.source_VSub.selectedIndex = this.cmdData.varLevel;

    if(this.cmdData.source_hasItems) {
      this.cellIDs.elems.source_iNameState.checked = true;
      this.cellIDs.elems.source_iNameState.onclick();

      if(this.cmdData.source_name.length > 0) {
        this.cellIDs.elems.source_iNameState = this.cmdData.source_name;
      }

      if(this.cmdData.source_match.length > 0) {
          this.cellIDs.elems.source_iNameState = this.cmdData.source_match;
      }
    }
    this.UpdateEditView();
  }

  UpdateEditView() {
    this.titleEdit.innerText = (this.editData || this.cmdData).Result;
  }

  // IDs for all the child cells/ data in the Source cell
  sourceCellIDs() {
      return {
        titleView: 'viewTx_' + this.itemID,
        titleEdit: 'editTx_' + this.itemID,

        cmdCondChkBox: 'condEnabled_' + this.itemID,
        condOptionalChkBox: 'condOpt_' + this.itemID,
        condReqOptions: 'condReqT_' + this.itemID,

        rootType: 'srcType_' + this.itemID,
        source_RSubCell: 'srcRTypeCell_' + this.itemID,
        source_RSub: 'srcRType_' + this.itemID,
        source_VSubCell: 'srcVTypeCell_' + this.itemID,
        source_VSub: 'srcVType_' + this.itemID,

        source_iNameCell: 'srcINameCell_' + this.itemID,
        source_iNameState: 'srcINameState_' + this.itemID,
        source_iNameOff: 'srcINameOff_' + this.itemID,
        source_iNameOn: 'srcINameOn_' + this.itemID,
        source_iName: 'srcIName_' + this.itemID,
        source_iMatchCell: 'srcIMatchCell_' + this.itemID,
        source_iMatchState: 'srcIMatchState_' + this.itemID,
        source_iMatchOff: 'srcIMatchOff_' + this.itemID,
        source_iMatchOn: 'srcIMatchOn_' + this.itemID,
        source_iMatch: 'srcIMatch_' + this.itemID,

        cmdActionChkBox: 'actEnabled_' + this.itemID,

        elems: {}
      }
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
    titleView.id = this.cellIDs.titleView;
    this.titleView = titleView;
    titleTextCell.appendChild(titleView);

    let titleEdit = document.createElement('div');
    titleEdit.id = this.cellIDs.titleEdit;
    this.titleEdit = titleEdit;
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
    btn.id = 'deleteBtn_' + this.itemID;
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
    btn.id = 'saveBtn_' + this.itemID;
    btn.style.display = 'none';
    btn.onclick = function() {
      self.titleView.innerText = self.titleEdit.innerText;
      toggleEditButton(self.itemID);
    };
    btn.innerText = 'Save';
    buttonsTD.appendChild(btn);

    btn = document.createElement('button');
    btn.type = 'button';
    btn.id = 'editCancelBtn_' + this.itemID;
    btn.onclick = function() {
      if(btn.innerText == 'Edit')
        self.editData = self.itemData.Clone()
      toggleEditButton(self.itemID);
      self.titleEdit.innerText = self.titleView.innerText;
    };
    btn.innerText = 'Edit';
    buttonsTD.appendChild(btn);

    return buttonsTD;
  }

  // body element for sequence editing controls
  createCmdEditor() {
    let cmdEditorDiv = document.createElement('div');
    cmdEditorDiv.id = 'editContent_' + this.itemID;
    cmdEditorDiv.style.display = 'none';

    let cmdTable = document.createElement('table');
    cmdTable.style.width = '100%';
    cmdTable.appendChild(this.cmdHeader());
    cmdTable.appendChild(this.cmdBody());

    cmdEditorDiv.appendChild(cmdTable);
    return cmdEditorDiv;
  }

  // "Conditional", "Source", "Action" headers
  cmdHeader() {
    var self = this;
    let cmdTableHead = document.createElement('thead');
    let cmdHeadRow = cmdTableHead.appendChild(document.createElement('tr'));

    // Conditional header
    let cmdCondHead = cmdHeadRow.appendChild(document.createElement('th'));
    cmdCondHead.style.width = '18%';
    cmdCondHead.appendChild(tooltipText(
      'Conditional',
      'Determines pre/ post actions of a source\'s results'
    ));

    let cmdCondChkBox = cmdCondHead.appendChild(document.createElement('input'));
    cmdCondChkBox.type = 'checkbox';
    cmdCondChkBox.id = this.cellIDs.cmdCondChkBox;
    this.cellIDs.elems.cmdCondChkBox = cmdCondChkBox;
    cmdCondChkBox.onclick = function() {
        self.toggleCondField(this);
        self.editData.isCond = this.checked;
        self.UpdateEditView();
    };

    // Source header
    let cmdSourceHead = cmdHeadRow.appendChild(document.createElement('th'));
    cmdSourceHead.appendChild(tooltipText(
      'Source',
      'What is the context of this step'
    ));

    // Action header
    let cmdActionHead = cmdHeadRow.appendChild(document.createElement('th'));
    cmdActionHead.style.width = '18%';
    cmdActionHead.appendChild(tooltipText(
      'Action',
      'Processing with the source content'
    ));

    let cmdActionChkBox = cmdActionHead.appendChild(document.createElement('input'));
    cmdActionChkBox.type = 'checkbox';
    cmdActionChkBox.id = this.cellIDs.cmdActionChkBox;
    this.cellIDs.elems.cmdActionChkBox = cmdActionChkBox;
    cmdActionChkBox.onclick = function() {
      self.toggleActionField(this);
      self.editData.act_hasItem = this.checked;
      self.UpdateEditView();
    };

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
    if (chkBox.checked) {
      this.actFieldID_On.style.display = 'inline';
      this.actFieldID_Off.style.display = 'none';
    } else {
      this.actFieldID_On.style.display = 'none';
      this.actFieldID_Off.style.display = 'inline';
    }
  }

  // command body; where the controls for changing the Command data are
  cmdBody() {
    let cmdTBody = document.createElement('tbody');
    let cmdRow = cmdTBody.appendChild(document.createElement('tr'));

    cmdRow.appendChild(this.condCell());
    cmdRow.appendChild(this.sourceCell());
//    cmdRow.appendChild(this.actCell());

    return cmdTBody;
  }

  // Cell containing the controls for "Conditional"
  condCell() {
    var self = this;
    let cell = document.createElement('td');

    let itemOffCell = cell.appendChild(document.createElement('div'));
    this.condFieldID_Off = itemOffCell;
    itemOffCell.id = 'parser_condOff_' + this.itemID;
    itemOffCell.appendChild(tooltipText(
      'Disabled',
      'This step\'s end state will not affect other steps'
    ));

    let itemOnCell = cell.appendChild(document.createElement('div'));
    this.condFieldID_On = itemOnCell;
    itemOnCell.id = 'parser_condOn_' + this.itemID;
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
    condOptionalChkBox.id = this.cellIDs.condOptionalChkBox;
    this.cellIDs.elems.condOptionalChkBox = condOptionalChkBox;
    condOptionalChkBox.onclick = function() {
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
    condReqOptions.id = this.cellIDs.condReqOptions;
    this.cellIDs.elems.condReqOptions = condReqOptions;
    condReqOptions.style.marginLeft = '0.2em';
    appendOption(condReqOptions, 'None', true);
    appendOption(condReqOptions, 'True');
    appendOption(condReqOptions, 'False');
    condReqOptions.onchange = function() {
        self.editData.condSrc = this.selectedIndex;
        self.UpdateEditView();
    };

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
    ctHead1.style.width = '30%';
    ctHead1.style.padding = '0';
    let ctHead2 = cTableHeadRow.appendChild(document.createElement('td'));
    ctHead2.style.padding = '0';

    // Add data row to the table
    this.sourceCellData(cellTable);

    return cell;
  }

  // Setup of the Source Cell rows (appending to cellTable)
  sourceCellData(cellTable) {
    let tableBody = cellTable.appendChild(document.createElement('tbody'));
    tableBody.appendChild(this.data_SourceTypeRow());
    tableBody.appendChild(this.data_htmlSubTypeRow());
    tableBody.appendChild(this.data_VarScopeCell());
    tableBody.appendChild(this.data_sourceIndex());
  }

  // Row for: Source's item type
  // - None, Request, Response, Variable, Uses
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
    dataSelect.id = this.cellIDs.rootType;
    this.cellIDs.elems.rootType = dataSelect;
    dataSelect.onchange = function() {
      let elms = self.cellIDs.elems;
      elms.source_RSubCell.style.display = 'none';
      elms.source_VSubCell.style.display = 'none';
      elms.source_iNameCell.style.display = 'none';
//      elms.source_iMatchCell.style.display = 'none';

      switch (this.selectedIndex) {
        case 1:
        case 2:
          elms.source_RSubCell.style.display = '';
          break;
        case 3:
          elms.source_VSubCell.style.display = '';
          elms.source_iNameCell.style.display = '';
//          elms.source_iMatchCell.style.display = '';
          break;
        case 4:
          elms.source_iNameCell.style.display = '';
//          elms.source_iMatchCell.style.display = '';
          break;
      }

      self.editData.srcType = this.selectedIndex;
      self.UpdateEditView();
    };

    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'Request');
    appendOption(dataSelect, 'Response');
    appendOption(dataSelect, 'Variable');
    appendOption(dataSelect, 'Uses');

    return row;
  }

  // Row for: Source (Request/ Response)'s sub type
  // - None, Head, Body
  data_htmlSubTypeRow() {
    var self = this;
    let row = document.createElement('tr');
    row.id = this.cellIDs.source_RSubCell;
    this.cellIDs.elems.source_RSubCell = row;
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'HTML sub-source type',
      'What data this command will interact with'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataSelect = cellData.appendChild(document.createElement('select'));
    dataSelect.id = this.cellIDs.source_RSub;
    dataSelect.onchange = function() {
      let elms = self.cellIDs.elems;
      elms.source_iNameCell.style.display = 'none';
//      elms.source_iMatchCell.style.display = 'none';

      switch (this.selectedIndex) {
        case 1:
          elms.source_iNameCell.style.display = '';
//          elms.source_iMatchCell.style.display = '';
          break;
        case 2:
//          elms.source_iMatchCell.style.display = '';
          break;
      }

      self.editData.isHead = this.selectedIndex == 1;
      self.editData.isBody = this.selectedIndex == 2;
      self.UpdateEditView();
    };

    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'Head');
    appendOption(dataSelect, 'Body');
    return row;
  }

  data_VarScopeCell() {
      let row = document.createElement('tr');
      row.id = this.cellIDs.source_VSubCell;
      this.cellIDs.elems.source_VSubCell = row;
      row.style.display = 'none'

      let cellHead = row.appendChild(document.createElement('td'));
      cellHead.appendChild(tooltipText(
        'Variable scope level',
        'Scope area of the search'
      ));

      let cellData = row.appendChild(document.createElement('td'));
      let dataSelect = cellData.appendChild(document.createElement('select'));
      dataSelect.id = this.cellIDs.source_VSub;
      this.cellIDs.elems.source_VSub = dataSelect;
      appendOption(dataSelect, '0: Self', true);
      appendOption(dataSelect, '1: Chapter');
      appendOption(dataSelect, '2: Test Bounds');

      dataSelect.onchange = function() {
        self.editData.varLevel = this.selectedIndex;
        self.UpdateEditView();
      };

      return row;
  }

  data_sourceIndex() {
    var self = this;
    let row = document.createElement('tr');
    row.id = this.cellIDs.source_iNameCell;
    this.cellIDs.elems.source_iNameCell = row;
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    let cellHeadChkBox = cellHead.appendChild(document.createElement('input'));
    cellHeadChkBox.id = this.cellIDs.source_iNameState;
    this.cellIDs.elems.source_iNameState = cellHeadChkBox;
    cellHeadChkBox.type = 'checkbox';
    cellHeadChkBox.style.marginRight = '1em';
    cellHeadChkBox.onclick = function() {
      if(this.checked) {
        self.cellIDs.elems.source_iNameOff.style.display = 'none';
        self.cellIDs.elems.source_iNameOn.style.display = '';
      } else {
        self.cellIDs.elems.source_iNameOff.style.display = '';
        self.cellIDs.elems.source_iNameOn.style.display = 'none';
      }

      self.editData.varLevel = this.selectedIndex;
      self.UpdateEditView();
    };

    cellHead.appendChild(tooltipText(
      'Source index',
      'Item Index within the source item.'
    ));

    let cellDataOff = row.appendChild(document.createElement('td'));
    cellDataOff.id = this.cellIDs.source_iNameOff;
    this.cellIDs.elems.source_iNameOff = cellDataOff;
    cellDataOff.appendChild(tooltipText(
          'Disabled',
          'No items within the source will be referenced.'
        ));

    let cellDataOn = row.appendChild(document.createElement('td'));
    cellDataOn.id = this.cellIDs.source_iNameOn;
    this.cellIDs.elems.source_iNameOn = cellDataOn;
    cellDataOn.style.display = 'none';

    let dataOnInput = cellDataOn.appendChild(document.createElement('input'));
    dataOnInput.id = this.cellIDs.source_iName;
    this.cellIDs.elems.source_iName = dataOnInput;
    dataOnInput.style.width = '100%';
    dataOnInput.placeholder = 'Reference Item'

    return row;
  }

}
