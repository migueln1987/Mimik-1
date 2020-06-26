
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
    this.varSearchUp = inData.varSearchUp || false;

    this.source_name = inData.source_name || null;
    this.source_match = inData.source_match || null;

    this.hasAction = inData.hasAction || false;
    this.act_name = inData.act_name || null;
    this.act_nExists = inData.act_nExists || false;
    this.act_nCount = inData.act_nCount || false;
    this.act_nResult = inData.act_nResult || false;
    this.act_nSpread = inData.act_nSpread || false;
    this.act_nSpreadType = inData.act_nSpreadType || -8;
    this.act_scopeLevel = inData.act_scopeLevel || false;
    this.act_match = inData.act_match || null;
  }

  Clone() {
    return Object.assign(new p4Parser(), JSON.parse(JSON.stringify(this)));
  }

  get source_hasItems() {
    return (this.source_name||'').length > 0 || (this.source_match||'').length > 0;
  }

  get act_hasItems() {
    return (this.act_name||'').length > 0 || (this.act_match||'').length > 0;
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
          if (this.varSearchUp)
            output += '^';
          output += 'var';
          break;

        case 4:
          output += 'use';
          break;
      }

      if(!valid)
        return "Invalid";

      if (this.source_hasItems) {
        if ((this.source_name||'').length > 0) output += '[' + this.source_name + ']';
        if ((this.source_match||'').length > 0) output += ':{' + this.source_match + '}';
      }

      if (this.act_hasItems) {
        output += '->';
        if ((this.act_name||'').length > 0) {
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
          if (this.act_nSpread) {
            switch(this.act_nSpreadType) {
              case -1:
                output += '_#';
                break;
              case -2:
                output += '_?';
                break;
            }
            if (this.act_nSpreadType >= 0) output += ('_#' + this.act_nSpreadType);
          }
        } else if ((this.act_match|'').length > 0) {
          output += '{' + this.act_match + '}'
        }
      }

      return output;
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
//      this.source_hasItems = (this.source_name != null || this.source_match != null);

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

// HostID (+ [{data}, {data}, ...]
class parserEditor {
  constructor(hostID) {
    this.hostID = hostID || uniqueRandom(rr => (data[rr] === undefined) ? rr : -1);
  }

  AddNewSeqList(inData) {
    var listRoot = document.getElementById('level_root');
    if (listRoot == null) return;

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
      commandContent.enableEditing();
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

    let makeData = () => {
      return {
        parent: this.hostID,
        newParent: -1,
        newIndex: -1,
        data: new p4Parser(),
        Clone: function() {
          let cloneObj = makeData();
          cloneObj.data = Object.assign(new p4Parser(),
            JSON.parse(JSON.stringify(this.data)));
          return cloneObj;
        },
      }
    };
    this.itemData = makeData();

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
    //    this.itemData.data = new p4Parser();
    //    this.cmdData = this.itemData.data;
    //
    //    this.cellIDs.elems.cmdCondChkBox.checked = this.cmdData.isCond;
    //    this.cellIDs.elems.cmdCondChkBox.onclick();
    //    this.cellIDs.elems.condOptionalChkBox.checked = this.cmdData.isOpt;
    //    this.cellIDs.elems.condOptionalChkBox.onclick();
    //
    //    this.cellIDs.elems.condReqOptions.selectedIndex = Math.max(0, this.cmdData.condSrc);
    //
    //    this.cellIDs.elems.rootType.selectedIndex = this.cmdData.srcType;
    //    this.cellIDs.elems.rootType.onchange();
    //
    //    if(this.cmdData.isHead)
    //      this.cellIDs.elems.source_RSub.selectedIndex = 1;
    //    else if(this.cmdData.isBody)
    //      this.cellIDs.elems.source_RSub.selectedIndex = 2;
    //    else
    //      this.cellIDs.elems.source_RSub.selectedIndex = 0;
    //    this.cellIDs.elems.source_RSub.onchange();
    //
    //    this.cellIDs.elems.source_VSub.selectedIndex = this.cmdData.varLevel;
    //
    //    if(this.cmdData.source_hasItems) {
    //      this.cellIDs.elems.source_iNameState.checked = true;
    //      this.cellIDs.elems.source_iNameState.onclick();
    //
    //      if(this.cmdData.source_name.length > 0) {
    //        this.cellIDs.elems.source_iNameState = this.cmdData.source_name;
    //      }
    //
    //      if(this.cmdData.source_match.length > 0) {
    //          this.cellIDs.elems.source_iNameState = this.cmdData.source_match;
    //      }
    //    }
    //    this.UpdateEditView();
  }

  UpdateEditView() {
    this.titleEdit.innerHTML = (this.editData || this.cmdData).Result();
  }

  // IDs for all the child cells/ data in the Source cell
  sourceCellIDs() {
    let ElemIDConfigs = [
      ['viewTx', []],
      ['editTx', []],
      ['root', ['Delete', 'Save', 'EditCancel', 'Content']],
      ['cond', ['Enabled', 'Off', 'On', 'Opt', 'ReqT']],
      ['src', ['Type']],

      ['srcRType', ['Row', 'Data']],
      ['srcVar', ['Type', 'Scope']],
      ['srcVarScp', ['Row', 'Data']],
      ['srcVarSrc', ['Row', 'Data']],

      ['srcIName', ['Row', 'State', 'Off', 'On', 'Data']],
      ['srcIMatch', ['Row', 'State', 'Off', 'On', 'Data']],

      ['act', ['Enabled', 'Off', 'On', 'typeOption']],
      ['actVar', ['Row', 'Scope', 'Data']],
      ['actVarPost', ['Row', 'Exists', 'Count', 'Result', 'Spread', 'Index']],
      ['actMatch', ['Row', 'Data']]
    ];

    let makeDataObjs = (rootName, subName = '') => {
      return {
        ID: `${rootName}${subName}_${this.itemID}`,
        Cell: null,
        setCell: function(elm) {
          this.Cell = elm;
          elm.id = this.ID;
        }
      };
    };

    let outData = {};
    ElemIDConfigs.forEach(elmConfig => {
      let name = elmConfig[0];
      let childs = elmConfig[1];
      let outObj = makeDataObjs(name);
      childs.forEach(cfg => {
        outObj[cfg] = makeDataObjs(name, cfg);
      });
      outData[name] = outObj;
    });

    return outData;
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
    this.cellIDs.viewTx.setCell(titleView);
    this.titleView = titleView;
    titleTextCell.appendChild(titleView);

    let titleEdit = document.createElement('div');
    this.cellIDs.editTx.setCell(titleEdit);
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
    this.cellIDs.root.Save.setCell(btn);
    btn.style.display = 'none';
    btn.onclick = function() {
      self.titleView.innerText = self.titleEdit.innerText;
      toggleEditButton(self.itemID);
    };
    btn.innerText = 'Save';
    buttonsTD.appendChild(btn);

    btn = document.createElement('button');
    this.EditingBtn = btn;
    btn.type = 'button';
    this.cellIDs.root.EditCancel.setCell(btn);
    btn.onclick = function() {
      if (btn.innerText == 'Edit') {
        self.TitleHandle.style.visibility = 'hidden';
        self.editData = self.itemData.Clone().data;
      } else {
        self.TitleHandle.style.visibility = 'unset';
      }

      toggleEditButton(self.itemID);
      //      self.titleEdit.innerText = self.titleView.innerText;
    };
    btn.innerText = 'Edit';
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
    this.cellIDs.cond.Enabled.setCell(cmdCondChkBox);
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
    //    cmdActionHead.style.width = '18%';
    cmdActionHead.appendChild(tooltipText(
      'Action',
      'Processing with the source content'
    ));

    let cmdActionChkBox = cmdActionHead.appendChild(document.createElement('input'));
    cmdActionChkBox.type = 'checkbox';
    this.cellIDs.act.Enabled.setCell(cmdActionChkBox);
    cmdActionChkBox.onclick = function() {
      self.toggleActionField(this);
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
    this.cellIDs.cond.ReqT.setCell(condReqOptions);
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
    this.cellIDs.src.Type.setCell(dataSelect);
    dataSelect.onchange = function() {
      let elms = self.cellIDs;
      let RTypeCell = self.cellIDs.srcRType.Row.Cell.style;
      let VScpCell = self.cellIDs.srcVarScp.Row.Cell.style;
      let VSrcCell = self.cellIDs.srcVarSrc.Row.Cell.style;
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
    this.cellIDs.srcRType.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'HTML sub-source type',
      'What data this command will interact with'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataSelect = cellData.appendChild(document.createElement('select'));
    this.cellIDs.srcRType.Data.setCell(dataSelect);
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

      self.editData.isHead = this.selectedIndex == 1;
      self.editData.isBody = this.selectedIndex == 2;
      if (row.style.display == '')
        self.UpdateEditView();
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcRType.Data.Cell.selectedIndex = 0;
        self.cellIDs.srcRType.Data.Cell.onchange();
      }
    }).observe(row, {
      attributes: true,
      attributeFilter: ['style']
    });

    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'Head');
    appendOption(dataSelect, 'Body');
    return row;
  }

  /*
   Scope of the variable source
   - Self, Chapter, Test Bounds
  */
  data_VarScopeCell() {
    var self = this;
    let row = document.createElement('tr');
    this.cellIDs.srcVarScp.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Variable scope level',
      'Scope area of the search'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataSelect = cellData.appendChild(document.createElement('select'));
    this.cellIDs.srcVarScp.Data.setCell(dataSelect);
    appendOption(dataSelect, '0: Self', true);
    appendOption(dataSelect, '1: Chapter');
    appendOption(dataSelect, '2: Test Bounds');

    dataSelect.onchange = function() {
      self.editData.varLevel = this.selectedIndex;
      self.UpdateEditView();
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcVarScp.Data.Cell.selectedIndex = 0;
        self.cellIDs.srcVarScp.Data.Cell.onchange();
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
    this.cellIDs.srcVarSrc.Row.setCell(row);
    row.style.display = 'none'

    let cellHead = row.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Variable hierarchy search',
      'When set, parent variables will also be searched'
    ));

    let cellData = row.appendChild(document.createElement('td'));
    let dataBox = cellData.appendChild(document.createElement('input'));
    dataBox.type = 'checkbox';
    this.cellIDs.srcVarSrc.Data.setCell(dataBox);
    dataBox.onclick = function() {
      self.editData.varSearchUp = this.checked;
      self.UpdateEditView();
    };

    new MutationObserver(function(targetView) {
      if (targetView[0].target.style.display == 'none') {
        self.cellIDs.srcVarSrc.Data.Cell.checked = false;
        self.cellIDs.srcVarSrc.Data.Cell.onclick();
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
    this.cellIDs.srcIName.State.setCell(cellHeadChkBox);
    cellHeadChkBox.type = 'checkbox';
    cellHeadChkBox.style.marginRight = '1em';
    cellHeadChkBox.onclick = function() {
      let srINameObj = self.cellIDs.srcIName;
      if (this.checked) {
        srINameObj.Off.Cell.style.display = 'none';
        srINameObj.On.Cell.style.display = '';
      } else {
        srINameObj.Data.Cell.value = '';
        self.editData.source_name = '';
        srINameObj.Off.Cell.style.display = '';
        srINameObj.On.Cell.style.display = 'none';
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
    this.cellIDs.srcIMatch.State.setCell(cellHeadChkBox);
    cellHeadChkBox.type = 'checkbox';
    cellHeadChkBox.style.marginRight = '1em';
    cellHeadChkBox.onclick = function() {
      let srIMatchObj = self.cellIDs.srcIMatch;
      if (this.checked) {
        srIMatchObj.Off.Cell.style.display = 'none';
        srIMatchObj.On.Cell.style.display = '';
      } else {
        self.editData.source_match = '';
        srIMatchObj.Data.Cell.value = '';
        srIMatchObj.Off.Cell.style.display = '';
        srIMatchObj.On.Cell.style.display = 'none';
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
        document.querySelector(`input[name='${self.cellIDs.act.typeOption.ID}']:checked`).checked = false
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
      let ActVarRow = self.cellIDs.actVar.Row.Cell.style;
      let ActMatchRow = self.cellIDs.actMatch.Row.Cell.style;
      if (option1Data.checked) {
        self.editData.act_name = '';
        self.editData.act_match = null;
        ActVarRow.display = '';
        ActMatchRow.display = 'none';
        self.UpdateEditView();
      } else if (option2Data.checked) {
        self.editData.act_name = null;
        self.editData.act_match = '';
        ActVarRow.display = 'none';
        ActMatchRow.display = '';
        self.UpdateEditView();
      }
    };

    let option1Cell = optionRow.appendChild(document.createElement('td'));
    let op1CellStyle = option1Cell.style;
    op1CellStyle.textAlign = 'center';
    op1CellStyle.borderRightWidth = '1px';
    op1CellStyle.borderRightStyle = 'solid';
    let info1Div = option1Cell.appendChild(tooltipText(
      'To Variable',
      'Where the command will put the data', 'left'
    ));
    info1Div.style.textAlign = 'center';
    let option1Data = option1Cell.appendChild(document.createElement('input'));
    option1Data.type = 'radio';
    option1Data.name = this.cellIDs.act.typeOption.ID;
    option1Data.onchange = () => onChangeFunction();

    let option2Cell = optionRow.appendChild(document.createElement('td'));
    option2Cell.style.textAlign = 'center';
    let info2Div = option2Cell.appendChild(tooltipText(
      'To Source',
      'What this command will do with data', 'left'
    ));
    info2Div.style.textAlign = 'center';
    let option2Data = option2Cell.appendChild(document.createElement('input'));
    option2Data.type = 'radio';
    option2Data.name = this.cellIDs.act.typeOption.ID;
    option2Data.onchange = () => onChangeFunction();

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
      self.editData.act_match = evt.target.value;
      self.UpdateEditView();
    };

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
    this.cellIDs.actVar.Scope.setCell(dataSelect);
    appendOption(dataSelect, '0: Self', true);
    appendOption(dataSelect, '1: Chapter');
    appendOption(dataSelect, '2: Test Bounds');

    dataSelect.onchange = function() {
      self.editData.act_scopeLevel = this.selectedIndex;
      self.UpdateEditView();
    };

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
      self.editData.act_nExists = this.checked;
      self.UpdateEditView();
    };

    return cellRow;
  }

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
      self.editData.act_nCount = this.checked;
      self.UpdateEditView();
    };

    return cellRow;
  }

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
      self.editData.act_nResult = this.checked;
      self.UpdateEditView();
    };

    return cellRow;
  }

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
    this.cellIDs.actVarPost.Spread.setCell(dataSelect);
    appendOption(dataSelect, 'None', true);
    appendOption(dataSelect, 'All');
    appendOption(dataSelect, 'Last');
    appendOption(dataSelect, 'Single index');
    dataSelect.onchange = function() {
      let indexSpreadCell = self.cellIDs.actVarPost.Index.Cell.style;
      switch(this.selectedIndex) {
        case 0:
        case 1:
        case 2:
          indexSpreadCell.display = 'none';
          self.editData.act_nSpreadType = -this.selectedIndex;
          if(self.editData.act_nSpreadType == 0)
            self.editData.act_nSpreadType = -8;
          self.UpdateEditView();
          break;

        case 3:
          indexSpreadCell.display = '';
          break;
      }
    };

    return cellRow;
  }

  actPostSpreadIndex() {
    var self = this;
    let cellRow = document.createElement('tr');
    this.cellIDs.actVarPost.Index.setCell(cellRow);
    cellRow.style.display = 'none';

    let cellHead = cellRow.appendChild(document.createElement('td'));
    cellHead.appendChild(tooltipText(
      'Index',
      'Export all the matched results as their own variable.\n' +
      'Ex: 0 => name_0, 13 => name_13, etc.',
      'left'
    ));

    let cellBody = cellRow.appendChild(document.createElement('td'));
    let dataOnInput = cellBody.appendChild(document.createElement('input'));
    dataOnInput.style.width = '100%';
    dataOnInput.placeholder = 'Index (0 -> xxx)'
    dataOnInput.oninput = function(evt) {
      let inputStr = parseInt(evt.target.value);
      if(isNaN(inputStr))
        inputStr = null;
      dataOnInput.value = inputStr;

      if(cellRow.style.display === '') {
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
