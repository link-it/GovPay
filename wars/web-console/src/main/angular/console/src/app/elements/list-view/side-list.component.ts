import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { LinkService } from "../../services/link.service";
import { GovpayService } from '../../services/govpay.service';
import { UtilService } from '../../services/util.service';
import { Voce } from '../../services/voce.service';

import { Parameters } from '../../classes/parameters';
import { Standard } from '../../classes/view/standard';
import { Dato } from '../../classes/view/dato';

import * as moment from 'moment';
import { ModalBehavior } from '../../classes/modal-behavior';
import { IExport } from '../../classes/interfaces/IExport';
import { ItemViewComponent } from '../item-view/item-view.component';
import { TwoCols } from '../../classes/view/two-cols';

declare let JSZip: any;
declare let FileSaver: any;

@Component({
  selector: 'link-side-list',
  templateUrl: './side-list.component.html',
  styleUrls: ['./side-list.component.scss']
})
export class SideListComponent implements OnInit, OnDestroy, IExport {

  @Input('list-data') listResults = [];
  @Input('enable-over-actions') iconOverActions: boolean = false;
  @Input('enable-fab-actions') fabAction: boolean = false;
  @Input('enable-multi-fab-actions') multiFabAction: boolean = false;
  @Input('is-loading-progress') _isLoading: boolean = false;
  @Output() _isLoadingChange: EventEmitter<boolean> = new EventEmitter();

  protected rsc: any;

  protected _lastResponse: any;
  protected _timerProgress: any;
  protected _csv: any;

  constructor(public ls: LinkService, public gps: GovpayService, public us: UtilService) { }

  ngOnInit() {
    this._unsubscribeExport();
    UtilService.exportSubscription = UtilService.exportBehavior.subscribe((value: string) => {
      if(value) {
        this.exportData(value);
      }
    });
    this.rsc = this.ls.getRouterStateConfig();
    this.iconOverActions = this.showIconOverActions();
    const _statusFab = this.showFabAction();
    this.fabAction = _statusFab.single;
    this.multiFabAction = _statusFab.multi;
    let _service: string = UtilService.DASHBOARD_LINKS_PARAMS.method;
    let _dashboard_link_query = UtilService.DASHBOARD_LINKS_PARAMS.params.map((item) => {
      return item.controller + '=' + item.value;
    }).join('&');
    this.getList(_service, _dashboard_link_query);
  }

  ngOnDestroy() {
    this._unsubscribeExport();
  }

  protected _unsubscribeExport() {
    if(UtilService.exportSubscription) {
      UtilService.exportSubscription.unsubscribe();
      UtilService.exportBehavior.next(null);
    }
  }

  /**
   * Get data list
   * @param {string} service
   * @param {string} query
   * @param {boolean} concat
   */
  getList(service?: string, query?: string, concat?: boolean) {
    service = (service || this.rsc.fullPath); // ROUTING - fullPath
    concat = (concat || false);
    if(!this._isLoading) {
      this._isLoading = true;
      this.gps.getDataService(service, query).subscribe(
        (_response) => {
          this._lastResponse = JSON.parse(JSON.stringify(_response.body));
          this.listResults = concat?this.listResults.concat(this._mapListResults(_response)):this._mapListResults(_response);
          this.gps.updateSpinner(false);
          this._isLoading = false;
          this._isLoadingChange.emit(this._isLoading);
        },
        (error) => {
          this._isLoading = false;
          this._isLoadingChange.emit(this._isLoading);
          this.gps.updateSpinner(false);
          this.us.onError(error);
        });
    }
  }

  /**
   * Get last result data
   * @returns {any}
   */
  getLastResult(): any {
    return this._lastResponse;
  }

  /**
   * Blue dialog behavior data mapper
   * @returns {ModalBehavior}
   */
  private blueDialogBehaviorDataMapper(): ModalBehavior {
    let _mb = new ModalBehavior();
    let _component;
    switch(this.rsc.fullPath) { // ROUTING - fullPath
      case UtilService.URL_PENDENZE:
        _mb.info = {
          dialogTitle: 'Nuova pendenza',
          templateName: UtilService.SCHEDA_PENDENZA
        };
        _component = this.ls.componentRefByName(UtilService.SCHEDA_PENDENZA);
        break;
      case UtilService.URL_REGISTRO_INTERMEDIARI:
        _mb.info = {
          dialogTitle: 'Nuovo intermediario',
          templateName: UtilService.INTERMEDIARIO
        };
        _component = this.ls.componentRefByName(UtilService.REGISTRO_INTERMEDIARI);
        break;
      case UtilService.URL_APPLICAZIONI:
        _mb.info = {
          dialogTitle: 'Nuova applicazione',
          templateName: UtilService.APPLICAZIONE
        };
        _component = this.ls.componentRefByName(UtilService.APPLICAZIONI);
        break;
      case UtilService.URL_OPERATORI:
        _mb.info = {
          dialogTitle: 'Nuovo operatore',
          templateName: UtilService.OPERATORE
        };
        _component = this.ls.componentRefByName(UtilService.OPERATORI);
        break;
      case UtilService.URL_DOMINI:
        _mb.info = {
          dialogTitle: Voce.NUOVO_ENTE_CREDITORE,
          templateName: UtilService.DOMINIO
        };
        _component = this.ls.componentRefByName(UtilService.DOMINI);
        break;
      case UtilService.URL_RUOLI:
        _mb.info = {
          dialogTitle: 'Nuovo ruolo',
          templateName: UtilService.RUOLO
        };
        _component = this.ls.componentRefByName(UtilService.RUOLI);
        break;
      case UtilService.URL_ENTRATE:
        _mb.info = {
          dialogTitle: 'Nuova entrata',
          templateName: UtilService.ENTRATA
        };
        _component = this.ls.componentRefByName(UtilService.ENTRATE);
        break;
      case UtilService.URL_INCASSI:
        _mb.info = {
          dialogTitle: 'Nuova riconciliazione',
          templateName: UtilService.INCASSO
        };
        _component = this.ls.componentRefByName(UtilService.INCASSI);
        break;
      case UtilService.URL_TRACCIATI:
        _mb.info = {
          dialogTitle: 'Nuovo tracciato',
          templateName: UtilService.TRACCIATO
        };
        _component = this.ls.componentRefByName(UtilService.TRACCIATI);
        break;
      case UtilService.URL_TIPI_PENDENZA:
        _mb.info = {
          dialogTitle: 'Nuovo tipo pendenza',
          templateName: UtilService.TIPO_PENDENZA
        };
        _component = this.ls.componentRefByName(UtilService.TIPI_PENDENZE);
        break;
      default:
        return null;
    }
    _mb.closure = this.refresh.bind(this);
    _mb.async_callback = _component.instance.save.bind(_component.instance);

    return _mb;
  }

  protected _mapListResults(response: HttpResponse<any>): any[] {
    let _mappedData: any;
    let _results = response.body['risultati']?response.body['risultati']:response.body;
    _mappedData = _results.map(function (item) {
      let _mappedElement = new Parameters();
      _mappedElement.model = this.mapNewItem(item);
      _mappedElement.jsonP = item;
      _mappedElement.type = this.classTemplate();
      return _mappedElement;
    }, this);

    return _mappedData;
  }

  protected _risultati(value: number = 0) {
    value = (value || 0);
    return (value != 1)?`Trovati ${value} risultati`:`Trovato ${value} risultato`;
  }

  protected _livClick(ref: any) {
    //console.log('click-tap', ref);
    let _url = this.ls.getRouterUrl()+UtilService.URL_DETTAGLIO;
    let _rc = this.ls.getRouterStateConfig(_url);
    if(_rc) {
      let _ivm = ref.getItemViewModel();

      switch(this.rsc.fullPath) { // ROUTING - fullPath
        case UtilService.URL_APPLICAZIONI:
          _rc.data.title = _ivm.jsonP.idA2A;
        break;
        default:
          _rc.data.title = _ivm.model.getStandardTitle();
      }
      _rc.data.info = _ivm.jsonP;
      this.ls.setRouterStateConfigData(_rc.data, _url);
      this.ls.navigateTo([_url]);
    }
  }

  protected _iconClick(event: any) {
    let _ivm = event.target.getItemViewModel();
    //console.log('icon-click', _ivm, event);
    let _rc = this.ls.getRouterStateConfig();
    let _service = null;
    let _json = null;
    let _query = null;
    let _method = null;
    switch(event.type) {
      case 'delete': {
        _method = UtilService.METHODS.DELETE;
        switch(_rc.data.type) {
          case UtilService.RUOLI:
            _service = UtilService.URL_RUOLI+'/'+_ivm.jsonP.id;
            break;
        }
        break;
      }
    }
    if(_service) {
      this.doIconAction(event.type, _rc.data.type, _service, _json, _query, _method);
    } else {
      console.warn('Service url unavailable.')
    }
  }

  protected doIconAction(action: string, type: string, service: string, json?: any, query?: any, method?: string) {
    this.gps.saveData(service, json, query, method).subscribe(
      () => {
        this.gps.updateSpinner(false);
        this.getList();
        /*switch(action) {
          case 'delete': {
            switch(type) {
              case UtilService.ACLS:
                break;
            }
            break;
          }
        }*/
      },
      (error) => {
        this.gps.updateSpinner(false);
        this.us.onError(error);
      });
  }

  protected showIconOverActions(): boolean {
    //TODO: Actions disattivate
    // let _iconOverActions: boolean = false;
    // switch(this.rsc.path) {
    //   case UtilService.URL_PENDENZE:
    //   case UtilService.URL_PAGAMENTI:
    //   case UtilService.URL_ACL:
    //     _iconOverActions = true;
    //     break;
    //   default:
    // }

    // return _iconOverActions;
    return false;
  }

  protected _overIcons(): string[] {
    let _icons: string[] = [];
    switch(this.rsc.path) {
      case UtilService.URL_RUOLI:
        _icons = ['delete'];
        break;
      default:
        _icons = ['file_download'];
    }

    return _icons;
  }

  protected showFabAction(): any {
    let _fabAction: any = { single: false, multi: false };
    switch(this.rsc.fullPath) { // ROUTING - fullPath
      case UtilService.URL_REGISTRO_INTERMEDIARI:
        _fabAction.single = UtilService.USER_ACL.hasPagoPA;
        break;
      case UtilService.URL_TRACCIATI:
        _fabAction.single = UtilService.USER_ACL.hasPagamentiePendenze;
        break;
      case UtilService.URL_APPLICAZIONI:
        _fabAction.single = UtilService.USER_ACL.hasApplicazioni;
        break;
      case UtilService.URL_OPERATORI:
      case UtilService.URL_RUOLI:
        _fabAction.single = UtilService.USER_ACL.hasRuoli;
        break;
      case UtilService.URL_DOMINI:
      case UtilService.URL_TIPI_PENDENZA:
        _fabAction.single = UtilService.USER_ACL.hasCreditore;
        break;
      case UtilService.URL_PENDENZE:
        _fabAction.single = UtilService.USER_ACL.hasPendenze && !UtilService.USER_ACL.hasPagamentiePendenze;
        _fabAction.multi = UtilService.USER_ACL.hasPendenze && UtilService.USER_ACL.hasPagamentiePendenze;
        break;
      case UtilService.URL_INCASSI:
        _fabAction.single = UtilService.USER_ACL.hasRendiIncassi;
        break;
      default:
    }

    return _fabAction;
  }

  protected _fabAction() {
    let _mb: ModalBehavior = this.blueDialogBehaviorDataMapper();
    if(_mb) {
      switch (_mb.info.templateName) {
        case UtilService.ENTRATA:
        case UtilService.INCASSO:
        case UtilService.TRACCIATO:
          UtilService.dialogBehavior.next(_mb);
          break;
        default:
        case UtilService.PENDENZA:
          UtilService.blueDialogBehavior.next(_mb);
      }
    } else {
      this.us.alert('Servizio non disponibile');
    }
  }

  protected _multiFabAction(event: any) {
    let _mb = new ModalBehavior();
    let _component;
    if(event.value === 'add') {
      _mb.info = {
        dialogTitle: 'Nuova pendenza',
        templateName: UtilService.SCHEDA_PENDENZA
      };
      _component = this.ls.componentRefByName(UtilService.SCHEDA_PENDENZA);
    }
    if(event.value === 'playlist_add') {
      _mb.info = {
        dialogTitle: 'Nuovo tracciato',
        templateName: UtilService.TRACCIATO
      };
      _component = this.ls.componentRefByName(UtilService.TRACCIATI);
    }
    _mb.closure = this.refresh.bind(this);
    _mb.async_callback = _component.instance.save.bind(_component.instance);

    if(event.value === 'playlist_add') {
      UtilService.dialogBehavior.next(_mb);
    }
    if(event.value === 'add') {
      UtilService.blueDialogBehavior.next(_mb);
    }
  }

  protected classTemplate(_service?: string): string {
    _service = (_service || this.rsc.path);
    let _classTemplate = '';
    switch(_service) {
      case UtilService.URL_GIORNALE_EVENTI:
        _classTemplate = UtilService.TWO_COLS;
      break;
    }

    return _classTemplate;
  }

  protected mapNewItem(item: any): Standard {
    let _std = new Standard();
    let _st, _date;
    switch(this.rsc.fullPath) { // ROUTING - fullPath
      case UtilService.URL_PENDENZE:
        const _iuv = (item.iuvAvviso)?item.iuvAvviso:item.iuvPagamento;
        _std.titolo = new Dato({ label: '',  value: item.causale });
        _std.sottotitolo = new Dato({ label: '',  value: Dato.concatStrings([ item.dominio.ragioneSociale, Voce.IUV+': '+_iuv ], ', ') });
        _std.importo = this.us.currencyFormat(item.importo);
        _std.stato = UtilService.STATI_PENDENZE[item.stato];
        break;
      case UtilService.URL_PAGAMENTI:
        _date = item.dataRichiestaPagamento?moment(item.dataRichiestaPagamento).format('DD/MM/YYYY [ore] HH:mm'):Voce.NON_PRESENTE;
        _st = new Dato({ label: Voce.DATA_RICHIESTA_PAGAMENTO+': ', value: _date });
        _std.titolo = new Dato({ value: item.nome });
        _std.sottotitolo = _st;
        _std.importo = this.us.currencyFormat(item.importo);
        _std.stato = UtilService.STATI_PAGAMENTO[item.stato];
        break;
      case UtilService.URL_REGISTRO_INTERMEDIARI:
        _std.titolo = new Dato({ label: item.denominazione });
        _std.sottotitolo = new Dato({ label: Voce.ID_INTERMEDIARIO+': ', value: item.idIntermediario });
        break;
      case UtilService.URL_APPLICAZIONI:
        _st = Dato.arraysToDato(
          [ Voce.ID_A2A, Voce.ABILITATO ],
          [ item.idA2A, UtilService.ABILITA[item.abilitato.toString()] ],
          ', '
        );
        _std.titolo = new Dato({ label: '',  value: item.principal });
        _std.sottotitolo = _st;
        break;
      case UtilService.URL_INCASSI:
        _std.titolo = new Dato({ label: Voce.ID_INCASSO+': ', value: item.idIncasso });
        _std.sottotitolo = new Dato({ label: Voce.CAUSALE+': ', value: item.causale });
        _std.importo = this.us.currencyFormat(item.importo);
        break;
      case UtilService.URL_GIORNALE_EVENTI:
        const _stdTC: TwoCols = new TwoCols();
        const _dataOraEventi = item.dataEvento?moment(item.dataEvento).format('DD/MM/YYYY [-] HH:mm:ss.SSS'):Voce.NON_PRESENTE;
        const _riferimento = this.us.mapRiferimentoGiornale(item);
        _stdTC.titolo = new Dato({ label: this.us.mappaturaTipoEvento(item.tipoEvento) });
        _stdTC.sottotitolo = new Dato({ label: _riferimento });
        _stdTC.stato = item.esito;
        _stdTC.data = _dataOraEventi;
        _std = _stdTC;
        break;
      case UtilService.URL_RISCOSSIONI:
        _st = Dato.arraysToDato(
          [ Voce.PENDENZA, Voce.IUV, Voce.ID_DOMINIO ],
          [ item.idVocePendenza, item.iuv, item.idDominio ],
          ', '
        );
        _std.titolo = new Dato({ label: Voce.IUR+': ', value: item.iur });
        _std.sottotitolo = _st;
        _std.importo = this.us.currencyFormat(item.importo);
        break;
      case UtilService.URL_RENDICONTAZIONI:
        let _tmpDR = item.dataRegolamento?moment(item.dataRegolamento).format('DD/MM/YYYY'):Voce.NON_PRESENTE;
        let tmpValue = [];
        tmpValue.push(_tmpDR);
        tmpValue.push(item.ragioneSocialeDominio?item.ragioneSocialeDominio:item.idDominio);
        tmpValue.push(item.ragioneSocialePsp?item.ragioneSocialePsp:item.idPsp);
        _st = Dato.arraysToDato(
          [ Voce.DATA, Voce.ENTE_CREDITORE_SIGLA, Voce.PSP ],
          tmpValue,
          ', '
        );
        _std.titolo = new Dato({ label: '',  value: item.idFlusso });
        _std.sottotitolo = _st;
        _std.importo = this.us.currencyFormat(item.importoTotale);
        break;
      case UtilService.URL_OPERATORI:
        _std.titolo = new Dato({ label: item.ragioneSociale });
        _std.sottotitolo = new Dato({ label: Voce.ABILITATO+': ', value: UtilService.ABILITA[item.abilitato.toString()] });
        break;
      case UtilService.URL_DOMINI:
        _std.titolo = new Dato({ label: '',  value: item.ragioneSociale });
        _std.sottotitolo = new Dato({ label: Voce.ID_DOMINIO+': ', value: item.idDominio });
        break;
      case UtilService.URL_RUOLI:
        _std.titolo = new Dato({ label: item.id });
        break;
      case UtilService.URL_ENTRATE:
        _st = Dato.arraysToDato(
          [ Voce.ID_ENTRATA, Voce.TIPO_CONTABILITA ],
          [ item.idEntrata, item.tipoContabilita ],
          ', '
        );
        _std.titolo = new Dato({ label: '',  value: item.descrizione });
        _std.sottotitolo = _st;
        break;
      case UtilService.URL_RPPS:
        _date = item.rpt.dataOraMessaggioRichiesta?moment(item.rpt.dataOraMessaggioRichiesta).format('DD/MM/YYYY'):Voce.NON_PRESENTE;
        let _subtitle = Dato.concatStrings([ Voce.DATA+': '+_date, Voce.CCP+': '+item.rpt.datiVersamento.codiceContestoPagamento ], ', ');
        _std.titolo = new Dato({ label: '', value: (item.rt && item.rt.istitutoAttestante)?item.rt.istitutoAttestante.denominazioneAttestante:Voce.NO_PSP });
        _std.sottotitolo = new Dato({ label: '', value: _subtitle });
        _std.stato = this._mapStato(item).stato;
        break;
      case UtilService.URL_TRACCIATI:
        let _tmpDC = item.dataOraCaricamento?moment(item.dataOraCaricamento).format('DD/MM/YYYY [ore] HH:mm'):Voce.NON_PRESENTE;
        _std.titolo = new Dato({ label: '',  value: item.nomeFile });
        _std.sottotitolo = new Dato({ label: Voce.DATA_CARICAMENTO+': ',  value: _tmpDC });
        _std.stato = UtilService.STATI_TRACCIATO[item.stato];
        break;
      case UtilService.URL_TIPI_PENDENZA:
        _st = Dato.arraysToDato(
          [ Voce.ID_TIPO_PENDENZA, Voce.TIPO,Voce.ABILITATO ],
          [ item.idTipoPendenza, item.tipo, UtilService.ABILITA[item.abilitato] ],
          ', '
        );
        _std.titolo = new Dato({ label: '',  value: item.descrizione });
        _std.sottotitolo = _st;
        break;
    }
    return _std;
  }

  _mapStato(item: any): any {
    let _map: any = { stato: '', motivo: '' };
    switch (item.stato) {
      case 'RT_ACCETTATA_PA':
        _map.stato = (item.rt)?UtilService.STATI_ESITO_PAGAMENTO[item.rt.datiPagamento.codiceEsitoPagamento]:'n/a';
        break;
      case 'RPT_RIFIUTATA_NODO':
      case 'RPT_RIFIUTATA_PSP':
      case 'RPT_ERRORE_INVIO_A_PSP':
        _map.stato = UtilService.STATI_RPP.FALLITO;
        _map.motivo = item.dettaglioStato+' - stato: '+item.stato;
        break;
      case 'RT_RIFIUTATA_PA':
      case 'RT_ESITO_SCONOSCIUTO_PA':
        _map.stato = UtilService.STATI_RPP.ANOMALO;
        _map.motivo = item.dettaglioStato+' - stato: '+item.stato;
        break;
      default:
        _map.stato = UtilService.STATI_RPP.IN_CORSO;
    }
    return _map;
  }

  protected refresh(mb: ModalBehavior) {
    if(mb && mb.info.viewModel) {
      let json = mb.info.viewModel;
      let _mappedElement;
      switch(mb.info.templateName) {
        case UtilService.INTERMEDIARIO:
          _mappedElement = new Parameters();
          _mappedElement.model = this.mapNewItem(json);
          _mappedElement.jsonP = json;
          _mappedElement?this.listResults.push(_mappedElement):null;
        break;
        case UtilService.INCASSO:
          _mappedElement = new Parameters();
          _mappedElement.model = this.mapNewItem(json);
          _mappedElement.jsonP = json;
          this.listResults.unshift(_mappedElement);
          const _component = this.ls.resolveComponentType(ItemViewComponent);
          _component.instance._componentData = _mappedElement;
          this._livClick(_component.instance);
        break;
        default:
          this.getList();
      }
    }
  }

  exportData(type: string) {
    this.gps.updateProgress(true, 0);
    let urls: string[] = [];
    let contents: string[] = [];
    let types: string[] = [];
    let _name: string = 'Export';

    switch(type) {
      case UtilService.EXPORT_PENDENZE:
        _name = UtilService.TXT_PENDENZE;
        break;
      case UtilService.EXPORT_PAGAMENTI:
        _name = UtilService.TXT_PAGAMENTI;
        break;
      case UtilService.EXPORT_RISCOSSIONI:
        _name = UtilService.TXT_RISCOSSIONI;
        break;
      case UtilService.EXPORT_GIORNALE_EVENTI:
        _name = UtilService.TXT_GIORNALE_EVENTI;
        break;
      case UtilService.EXPORT_INCASSI:
        _name = UtilService.TXT_INCASSI;
        break;
      case UtilService.EXPORT_RENDICONTAZIONI:
        _name = UtilService.TXT_RENDICONTAZIONI;
        break;
    }
    let _preloadedData:any = this.getLastResult();
    if(_preloadedData['prossimiRisultati']) {
      let _query = _preloadedData['prossimiRisultati'].split('?');
      _query[_query.length - 1] = _query[_query.length - 1].split('&').filter((_p) => {
        return _p.indexOf('pagina') == -1;
      }).join('&');
      for(let i = _preloadedData.pagina + 1; i <= _preloadedData.numPagine; i++) {
        urls.push(_query.join('?') + '&pagina=' + i);
        contents.push('application/json');
        types.push('json');
      }
    }
    let cachedCalls = this.listResults.map((result) => {
      return result.jsonP;
    });
    if(_preloadedData.pagina == _preloadedData.numPagine) {
      this.saveFile(cachedCalls, { type: type, name: _name }, '.csv');
    } else {
      this.gps.multiExportService(urls, contents, types).subscribe(function (_responses) {
          _responses.forEach((response) => {
            cachedCalls = cachedCalls.concat(response.body.risultati);
          });
          this.saveFile(cachedCalls, { type: type, name: _name }, '.csv');
        }.bind(this),
        (error) => {
          this.gps.updateSpinner(false);
          this.us.onError(error);
        });
    }
  }

  saveFile(data: any, structure: any, ext: string) {
    this._csv = { name: structure.name + ext, data: null, structure: structure };
    this.jsonToCsv(structure.type, data);
  }

  jsonToCsv(_name: string, _jsonData: any) {

    clearInterval(this._timerProgress);

    let _properties = {};
    switch(_name) {
      case UtilService.EXPORT_PENDENZE:
        _properties = {
          idA2A: 'idA2A', idPendenza: 'idPendenza', dominio_idDominio: 'idDominio', dominio_ragioneSociale: 'anagraficaDominio',
          numeroAvviso: 'numeroAvviso', importo: 'importo', dataCaricamento: 'dataCaricamento', dataValidita: 'dataValidita',
          dataScadenza: 'dataScadenza', tassonomiaAvviso: 'tassonomiaAvviso', stato: 'stato'
        };
        this.filteredJson(_properties, _jsonData, [ 'dataScadenza' ], { dataScadenza: '' });
        break;
      case UtilService.EXPORT_PAGAMENTI:
        _properties = {
          id: 'id', dataRichiestaPagamento: 'dataRichiestaPagamento', importo: 'importo', stato: 'stato',
          soggettoVersante_identificativo: 'idSoggettoVersante', soggettoVersante_anagrafica: 'anagraficaSoggettoVersante',
          contoAddebito_iban: 'contoAddebito'
        };
        this.filteredJson(_properties, _jsonData);
        break;
      case UtilService.EXPORT_RISCOSSIONI:
        _properties = {
          idDominio: 'idDominio', iuv: 'iuv', iur: 'iur', indice: 'indice', pendenza: 'pendenza', idVocePendenza: 'idVocePendenza',
          rpp: 'rpp', stato: 'stato', tipo: 'tipo', importo: 'importo', data: 'data', commissioni: 'commissioni', incasso: 'incasso'
        };
        this.filteredJson(_properties, _jsonData);
        break;
      case UtilService.EXPORT_GIORNALE_EVENTI:
      case UtilService.EXPORT_INCASSI:
        this.fullJson(_jsonData);
        break;
      case UtilService.EXPORT_RENDICONTAZIONI:
        _jsonData = _jsonData.map((item) => {
          let _segnalazioni = item.segnalazioni;
          item.segnalazioni = _segnalazioni.map((s) => {
            return s.codice;
          }).join(', ');
          return item;
        });
        this.fullJson(_jsonData);
        break;
    }
  }

  protected filteredJson(_properties: any, _jsonData: any, _customProperties: string[] = [], _defaultValues?: any) {
    let _csv: string = '';
    _csv = Object.keys(_properties).map((key) => {
      return '"'+_properties[key]+'"';
    }).join(', ')+'\r\n';

    this._csv.data = _csv;
    this._timerProgress = setInterval(() => {
      if(this._csv.data) {
        clearInterval(this._timerProgress);
        this._generateZip();
      }
    }, 1200);

    for(let _index = 0; _index < _jsonData.length; _index++) {
      setTimeout(() => {
        let row: string[] = [];
        Object.keys(_properties).forEach((key) => {
          let _defaultValue = 'n/a';
          if(_customProperties.indexOf(key) != -1) {
            _defaultValue = _defaultValues[key];
          }
          row.push('"'+this.getJsonProperty(key, _jsonData[_index], _defaultValue)+'"');
        }, this);
        _csv += row.join(', ') + '\r\n';

        let _progress = _index * (100/_jsonData.length);
        this.gps.updateProgress(true, _progress);
        if(_index == (_jsonData.length - 1)) {
          this._csv.data = _csv;
        }
      }, 1000);
    }
  }

  protected fullJson(_jsonData: any) {
    let _csv: string = '';
    let _keys = [];

    this._csv.data = _csv;
    this._timerProgress = setInterval(() => {
      if(this._csv.data) {
        clearInterval(this._timerProgress);
        this._generateZip();
      }
    }, 1200);

    for(let _index = 0; _index < _jsonData.length; _index++) {
      setTimeout(() => {

        let _json = _jsonData[_index];
        if(_index == 0) {
          _keys = Object.keys(_json);
          let _mappedKeys = _keys.map((key) => {
            return '"'+key+'"';
          });
          _csv = _mappedKeys.join(', ')+'\r\n';
        }
        let row: string[] = [];
        _keys.forEach((_key) => {
          let value = '';
          try {
            value = (_json[_key] || 'n/a');
          } catch(e) {
            value = 'n/a';
          }
          row.push('"'+value+'"');
        });
        _csv += row.join(', ')+'\r\n';

        let _progress = _index * (100/_jsonData.length);
        this.gps.updateProgress(true, _progress);
        if(_index == (_jsonData.length - 1)) {
          this._csv.data = _csv;
        }
      }, 1000);
    }
  }

  protected _generateZip() {
    this.gps.updateProgress(true, 100);
    let zip = new JSZip();
    zip.file(this._csv.name, this._csv.data);
    zip.generateAsync({type: 'blob'}).then(function (zipData) {
      FileSaver(zipData, this._csv.structure.name + '.zip');
      this.gps.updateProgress(false);
    }.bind(this));
  }

  protected getJsonProperty(value: string, property: any, _defaultValue: string = 'n/a'): any {
    value.split('_').forEach((value) => {
      try {
        property = (property[value] || _defaultValue);
      } catch(e) {
        property = _defaultValue;
      }
    });

    return property;
  }
}
