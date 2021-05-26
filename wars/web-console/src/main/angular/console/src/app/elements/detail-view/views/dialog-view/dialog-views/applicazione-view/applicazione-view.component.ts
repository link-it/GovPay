import { AfterViewInit, Component, Input, OnInit } from '@angular/core';
import { IFormComponent } from '../../../../../../classes/interfaces/IFormComponent';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { UtilService } from '../../../../../../services/util.service';
import { Voce } from '../../../../../../services/voce.service';
import { GovpayService } from '../../../../../../services/govpay.service';
import { Standard } from '../../../../../../classes/view/standard';
import { Dato } from '../../../../../../classes/view/dato';
import { Parameters } from '../../../../../../classes/parameters';
import { ModalBehavior } from '../../../../../../classes/modal-behavior';
import { IModalDialog } from '../../../../../../classes/interfaces/IModalDialog';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'link-applicazione-view',
  templateUrl: './applicazione-view.component.html',
  styleUrls: ['./applicazione-view.component.scss']
})
export class ApplicazioneViewComponent implements IModalDialog, IFormComponent, OnInit, AfterViewInit {

  @Input() fGroup: FormGroup;
  @Input() json: any;
  @Input() modified: boolean = false;

  protected voce = Voce;
  protected BASIC = UtilService.TIPI_AUTENTICAZIONE.basic;
  protected SSL = UtilService.TIPI_AUTENTICAZIONE.ssl;
  protected CLIENT = UtilService.TIPI_SSL.client;
  protected SERVER = UtilService.TIPI_SSL.server;

  protected versioni: any[] = UtilService.TIPI_VERSIONE_API;

  protected _isBasicAuth: boolean = false;
  protected _isSslAuth: boolean = false;
  protected _isSslClient: boolean = false;
  protected ruoli = [];
  protected domini = [];
  protected tipiPendenza = [];

  protected _attivaGestionePassword: boolean = false;

  constructor(public gps: GovpayService, public us: UtilService) {
    this._attivaGestionePassword = !!(UtilService.GESTIONE_PASSWORD && UtilService.GESTIONE_PASSWORD.ENABLED);
  }

  ngOnInit() {
    this.elencoDominiPendenzeRuoli();

    if (this._attivaGestionePassword) {
      this.fGroup.addControl('pwd_ctrl', new FormControl(''));
    }
    this.fGroup.addControl('idA2A_ctrl', new FormControl('', Validators.required));
    this.fGroup.addControl('principal_ctrl', new FormControl('', Validators.required));
    this.fGroup.addControl('abilita_ctrl', new FormControl(false));
    this.fGroup.addControl('codificaIuv_ctrl', new FormControl(''));
    this.fGroup.addControl('regExpIuv_ctrl', new FormControl(''));
    this.fGroup.addControl('generazioneIuvInterna_ctrl', new FormControl(false));
    this.fGroup.addControl('apiPagamenti_ctrl', new FormControl(false));
    this.fGroup.addControl('apiPendenze_ctrl', new FormControl(false));
    this.fGroup.addControl('apiRagioneria_ctrl', new FormControl(false));

    this.fGroup.addControl('url_ctrl', new FormControl(''));
    this.fGroup.addControl('versioneApi_ctrl', new FormControl({ value: '', disabled: true }, null));
    this.fGroup.addControl('auth_ctrl', new FormControl({ value: '', disabled: true }));

    this.fGroup.addControl('tipoPendenza_ctrl', new FormControl(''));
    this.fGroup.addControl('ruoli_ctrl', new FormControl(''));
  }

  ngAfterViewInit() {
    setTimeout(() => {
      if (this.json) {
        if (this._attivaGestionePassword) {
          this.fGroup.controls['pwd_ctrl'].clearValidators();
          this.fGroup.controls['pwd_ctrl'].setErrors(null);
        }
        if(this.json.idA2A) {
          this.fGroup.controls['idA2A_ctrl'].disable();
          this.fGroup.controls['idA2A_ctrl'].setValue(this.json.idA2A);
        }
        (this.json.principal)?this.fGroup.controls['principal_ctrl'].setValue(this.json.principal):null;
        (this.json.abilitato)?this.fGroup.controls['abilita_ctrl'].setValue(this.json.abilitato):null;
        (this.json.apiPagamenti)?this.fGroup.controls['apiPagamenti_ctrl'].setValue(this.json.apiPagamenti):null;
        (this.json.apiPendenze)?this.fGroup.controls['apiPendenze_ctrl'].setValue(this.json.apiPendenze):null;
        (this.json.apiRagioneria)?this.fGroup.controls['apiRagioneria_ctrl'].setValue(this.json.apiRagioneria):null;
        if (this.json.codificaAvvisi) {
          (this.json.codificaAvvisi.codificaIuv)?this.fGroup.controls['codificaIuv_ctrl'].setValue(this.json.codificaAvvisi.codificaIuv):null;
          (this.json.codificaAvvisi.regExpIuv)?this.fGroup.controls['regExpIuv_ctrl'].setValue(this.json.codificaAvvisi.regExpIuv):null;
          (this.json.codificaAvvisi.generazioneIuvInterna)?this.fGroup.controls['generazioneIuvInterna_ctrl'].setValue(this.json.codificaAvvisi.generazioneIuvInterna):null;
        }
        if (this.json.servizioIntegrazione) {
          const item = this.json.servizioIntegrazione;
          this._isSslAuth = false;
          this._isBasicAuth = false;
          if (item.url) {
            this.fGroup.controls['url_ctrl'].setValue(item.url);
            this.fGroup.controls['versioneApi_ctrl'].enable();
            this.fGroup.controls['auth_ctrl'].enable();
          }
          if (item.versioneApi) {
            this.fGroup.controls['versioneApi_ctrl'].setValue(item.versioneApi);
          }
          this.fGroup.controls['auth_ctrl'].setValue('');
          if (item.auth) {
            if (item.auth.hasOwnProperty('username')) {
              this._isBasicAuth = true;
              this.fGroup.controls['auth_ctrl'].setValue(this.BASIC);
              this.addBasicControls();
              this.fGroup.controls['username_ctrl'].setValue(item.auth.username);
              this.fGroup.controls['password_ctrl'].setValue(item.auth.password);
            }
            if (item.auth.hasOwnProperty('tipo')) {
              this._isSslAuth = true;
              this.fGroup.controls['auth_ctrl'].setValue(this.SSL);
              this.addSslControls();
              this.fGroup.controls['ssl_ctrl'].setValue(item.auth.tipo === this.CLIENT);
              this.fGroup.controls['tsLocation_ctrl'].setValue(item.auth.tsLocation);
              this.fGroup.controls['tsPassword_ctrl'].setValue(item.auth.tsPassword);
              if (item.auth.tipo === this.CLIENT) {
                this.addSslClientControls();
                this.fGroup.controls['ksLocation_ctrl'].setValue(item.auth.ksLocation);
                this.fGroup.controls['ksPassword_ctrl'].setValue(item.auth.ksPassword);
                this._isSslClient = true;
              }
            }
          }
        }
        if (this.json.tipiPendenza) {
          this.fGroup.controls[ 'tipoPendenza_ctrl' ].setValue(this.json.tipiPendenza);
        }
        if (this.json.ruoli) {
          this.fGroup.controls[ 'ruoli_ctrl' ].setValue(this.json.ruoli);
        }
      }
    });
  }

  protected _inputchanged(event) {
    if (this._attivaGestionePassword) {
      if (event.currentTarget.value === '') {
        this.fGroup.controls['pwd_ctrl'].clearValidators();
        this.fGroup.controls['pwd_ctrl'].setErrors(null);
      } else {
        this.fGroup.controls['pwd_ctrl'].setValidators(Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?!.*[\s]).{8,}$/));
      }
      this.fGroup.controls['pwd_ctrl'].updateValueAndValidity();
    }
  }

  protected elencoDominiPendenzeRuoli() {
    let _services: string[] = [];
    _services.push(UtilService.URL_DOMINI);
    _services.push(UtilService.URL_TIPI_PENDENZA+'?'+UtilService.QUERY_ESCLUDI_METADATI_PAGINAZIONE);
    _services.push(UtilService.URL_RUOLI);
    this.gps.updateSpinner(true);
    this.gps.forkService(_services).subscribe(function (_response) {
        if(_response) {
          this.domini = (this.json)?this.elencoDominiMap(this.json.domini || []):[];
          this.tipiPendenza = _response[1].body.risultati;
          if (UtilService.USER_ACL.hasCreditore && UtilService.USER_ACL.hasTuttiTipiPendenza) {
            this.tipiPendenza.unshift({ descrizione: UtilService.TUTTI_TIPI_PENDENZA.label, idTipoPendenza: UtilService.TUTTI_TIPI_PENDENZA.value });
            this.tipiPendenza.unshift({ descrizione: UtilService.AUTODETERMINAZIONE_TIPI_PENDENZA.label, idTipoPendenza: UtilService.AUTODETERMINAZIONE_TIPI_PENDENZA.value });
          }
          this.ruoli = _response[2].body.risultati;
          this.gps.updateSpinner(false);
        }
      }.bind(this),
      (error) => {
        this.gps.updateSpinner(false);
        this.us.onError(error);
      });
  }

  protected elencoDominiMap(data: any[]) {
    return data.map(function(item) {
      let p = new Parameters();
      p.jsonP = item;
      p.model = this.mapNewItem(item);
      return p;
    }, this);
  }

  /**
   * Map item Dominio
   * @param item
   * @returns {Standard}
   */
  protected mapNewItem(item: any): Standard {
    let _values = (item.unitaOperative || []).map(uo => {
      return uo.ragioneSociale;
    });
    let _std = new Standard();
    let _st = new Dato({
      label: `${Voce.UNITA_OPERATIVE}: `
    });
    if (_values.length !== 0) {
      _st.value = _values.join(', ');
    } else {
      _st.value = (item.idDominio === '*')?Voce.TUTTE:Voce.NESSUNA
    }
    _std.titolo = new Dato({ label: item.ragioneSociale });
    _std.sottotitolo = _st;

    return  _std;
  }

  /**
   * Add (Enti,Unità operative)
   * @param {boolean} mode
   * @param _viewModel
   * @private
   */
  protected _addEnteUO(mode: boolean = false, _viewModel?: any) {
    let _mb: ModalBehavior = new ModalBehavior();
    _mb.editMode = mode;
    _mb.info = {
      viewModel: _viewModel,
      parent: this,
      dialogTitle: 'Nuova autorizzazione',
      templateName: UtilService.AUTORIZZAZIONE_ENTE_UO
    };
    _mb.closure = this.refresh.bind(this);
    UtilService.dialogBehavior.next(_mb);
  }

  protected _iconClick(ref: any, event: any) {
    let _ivm = ref.getItemViewModel();
    switch(event.type) {
      case 'delete':
        this.domini = this.domini.filter(d => {
          return d.jsonP.idDominio !== _ivm.jsonP.idDominio;
        });
        break;
    }
  }

  refresh(mb: ModalBehavior) {
    this.modified = false;
    if(mb && mb.info && mb.info.viewModel) {
      this.modified = true;
      let p = new Parameters();
      let json = mb.info.viewModel;
      const _newItem = {
        idDominio: json.dominio.idDominio,
        ragioneSociale: json.dominio.ragioneSociale,
        unitaOperative: json.unitaOperative || []
      };
      p.jsonP = _newItem;
      p.model = this.mapNewItem(_newItem);
      (_newItem.idDominio === '*')?this.domini = [p]:this.domini.push(p);
      if (this.domini.length > 1) {
        this.domini = this.domini.filter(el => {
          return el.jsonP.idDominio !== '*';
        });
      }
    }
  }

  save(responseService: BehaviorSubject<any>, mb: ModalBehavior) {}

  protected pendenzaCmpFn(p1: any, p2: any): boolean {
    return (p1 && p2)?(p1.idTipoPendenza === p2.idTipoPendenza):(p1 === p2);
  }

  protected ruoliCmpFn(p1: any, p2: any): boolean {
    return (p1 && p2)?(p1.id === p2.id):(p1 === p2);
  }

  protected _onUrlChange(trigger: any, targets: string) {
    targets.split('|').forEach((_target, _ti) => {
      const _tc = this.fGroup.controls[_target];
      _tc.clearValidators();
      if(trigger.value.trim() !== '') {
        _tc.enable();
        _tc.setValidators((_ti==0)?Validators.required:null);
      } else {
        _tc.setValue('');
        _tc.disable({ onlySelf: true });
        this._isSslAuth = false;
        this._isBasicAuth = false;
        this.removeBasicControls();
        this.removeSslControls();
      }
      _tc.updateValueAndValidity();
    });
  }

  protected _onAuthChange(target: any) {
    this._isSslAuth = false;
    this._isBasicAuth = false;
    this.removeBasicControls();
    this.removeSslControls();
    switch(target.value) {
      case this.BASIC:
        this.addBasicControls();
        this._isBasicAuth = true;
        break;
      case this.SSL:
        this.addSslControls();
        this._isSslAuth = true;
        break;
    }
  }

  protected _onTypeChange(target) {
    this._isSslClient = false;
    this.removeSslClientControls();
    if (target.checked === true) {
      this._isSslClient = true;
      this.addSslClientControls();
    }
  }

  protected addBasicControls() {
    this.fGroup.addControl('username_ctrl', new FormControl('', Validators.required));
    this.fGroup.addControl('password_ctrl', new FormControl('', Validators.required));
  }

  protected addSslControls() {
    this.fGroup.addControl('ssl_ctrl', new FormControl(false, Validators.required));
    this.fGroup.addControl('tsLocation_ctrl', new FormControl('', Validators.required));
    this.fGroup.addControl('tsPassword_ctrl', new FormControl('', Validators.required));
  }

  protected addSslClientControls() {
    this.fGroup.addControl('ksLocation_ctrl', new FormControl('', Validators.required));
    this.fGroup.addControl('ksPassword_ctrl', new FormControl('', Validators.required));
  }

  protected removeSslClientControls() {
    this.fGroup.removeControl('ksLocation_ctrl');
    this.fGroup.removeControl('ksPassword_ctrl');
  }

  protected removeBasicControls() {
    this.fGroup.removeControl('username_ctrl');
    this.fGroup.removeControl('password_ctrl');
  }

  protected removeSslControls() {
    this.fGroup.removeControl('ssl_ctrl');
    this.fGroup.removeControl('ksLocation_ctrl');
    this.fGroup.removeControl('ksPassword_ctrl');
    this.fGroup.removeControl('tsLocation_ctrl');
    this.fGroup.removeControl('tsPassword_ctrl');
  }

  /**
   * Interface IFormComponent: Form controls to json object
   * @returns {any}
   */
  mapToJson(): any {
    let _info = this.fGroup.value;
    let _json:any = {};
    _json.idA2A = (!this.fGroup.controls['idA2A_ctrl'].disabled)?_info['idA2A_ctrl']:this.json.idA2A;
    if (UtilService.GESTIONE_PASSWORD && UtilService.GESTIONE_PASSWORD.ENABLED) {
      if (_info['pwd_ctrl']) {
        _json.password = _info['pwd_ctrl'];
      }
    }
    _json.abilitato = _info['abilita_ctrl'];
    _json.apiPagamenti = _info['apiPagamenti_ctrl'];
    _json.apiPendenze = _info['apiPendenze_ctrl'];
    _json.apiRagioneria = _info['apiRagioneria_ctrl'];
    _json.principal = (_info['principal_ctrl'])?_info['principal_ctrl']:null;
    _json.codificaAvvisi = {
      codificaIuv: (_info['codificaIuv_ctrl'])?_info['codificaIuv_ctrl']:null,
      regExpIuv: (_info['regExpIuv_ctrl'])?_info['regExpIuv_ctrl']:null,
      generazioneIuvInterna: (_info['generazioneIuvInterna_ctrl'])?_info['generazioneIuvInterna_ctrl']:false
    };

      _json.servizioIntegrazione = {
        auth: null,
        url: _info['url_ctrl']?_info['url_ctrl']:null,
        versioneApi: _info['versioneApi_ctrl']?_info['versioneApi_ctrl']:null
      };
      if(_info.hasOwnProperty('username_ctrl')) {
        _json.servizioIntegrazione.auth = {
          password: _info['password_ctrl'],
          username: _info['username_ctrl']
        };
      }
      if(_info.hasOwnProperty('ssl_ctrl')) {
        _json.servizioIntegrazione.auth = {
          tipo: _info['ssl_ctrl']?this.CLIENT:this.SERVER,
          tsLocation: _info['tsLocation_ctrl'],
          tsPassword: _info['tsPassword_ctrl'],
          ksLocation: '',
          ksPassword: ''
        };
        if(_info['ssl_ctrl'] === true) {
          _json.servizioIntegrazione.auth.ksLocation = _info['ksLocation_ctrl'];
          _json.servizioIntegrazione.auth.ksPassword = _info['ksPassword_ctrl'];
        } else {
          delete _json.servizioIntegrazione.auth.ksLocation;
          delete _json.servizioIntegrazione.auth.ksPassword;
        }
      }
      if(_json.servizioIntegrazione.auth == null) { delete _json.servizioIntegrazione.auth; }
      if(_json.servizioIntegrazione.url == null) {
        _json.servizioIntegrazione = null;
      }

    //_json.domini = (_info['dominio_ctrl'])?_info['dominio_ctrl']:[];
    _json.domini = this.domini || [];
    _json.tipiPendenza = (_info['tipoPendenza_ctrl'])?_info['tipoPendenza_ctrl']:[];
    _json.ruoli = (_info['ruoli_ctrl'])?_info['ruoli_ctrl']:[];
    _json.acl = this.json?(this.json.acl || []):[];

    return _json;
  }
}
