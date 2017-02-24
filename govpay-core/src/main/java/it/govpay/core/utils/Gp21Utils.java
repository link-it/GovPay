/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC 
 * http://www.gov4j.it/govpay
 * 
 * Copyright (c) 2014-2017 Link.it srl (http://www.link.it).
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.govpay.core.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.openspcoop2.generic_project.exception.ServiceException;

import it.govpay.bd.BasicBD;
import it.govpay.model.Iuv;
import it.govpay.bd.model.Fr;
import it.govpay.bd.model.Pagamento;
import it.govpay.bd.model.RendicontazionePagamento;
import it.govpay.bd.model.Rpt;
import it.govpay.bd.model.Rr;
import it.govpay.bd.model.Versamento;
import it.govpay.model.Versionabile;
import it.govpay.model.Versionabile.Versione;
import it.govpay.bd.pagamento.filters.VersamentoFilter.SortFields;
import it.govpay.servizi.commons.Canale;
import it.govpay.servizi.commons.EsitoTransazione;
import it.govpay.servizi.commons.FlussoRendicontazione;
import it.govpay.servizi.commons.IuvGenerato;
import it.govpay.servizi.commons.ModelloPagamento;
import it.govpay.servizi.commons.StatoRevoca;
import it.govpay.servizi.commons.StatoTransazione;
import it.govpay.servizi.commons.StatoVersamento;
import it.govpay.servizi.commons.TipoAllegato;
import it.govpay.servizi.commons.TipoRendicontazione;
import it.govpay.servizi.commons.TipoVersamento;
import it.govpay.servizi.commons.Transazione;
import it.govpay.servizi.commons.Pagamento.Allegato;
import it.govpay.servizi.gpprt.GpChiediListaVersamentiResponse.Versamento.SpezzoneCausaleStrutturata;
import it.govpay.servizi.gpprt.GpChiediStatoRichiestaStornoResponse.Storno;
import it.govpay.servizi.gprnd.GpChiediListaFlussiRendicontazioneResponse;

public class Gp21Utils {

	public static Transazione toTransazione(Versione versione, Rpt rpt, BasicBD bd) throws ServiceException {
		Transazione t = new Transazione();
		Canale canale = new Canale();
		canale.setCodCanale(rpt.getCanale(bd).getCodCanale());
		canale.setCodPsp(rpt.getPsp(bd).getCodPsp());
		canale.setTipoVersamento(TipoVersamento.valueOf(rpt.getCanale(bd).getTipoVersamento().getCodifica()));
		t.setCanale(canale);
		t.setCcp(rpt.getCcp());
		t.setCodDominio(rpt.getCodDominio());
		t.setDescrizioneStato(rpt.getDescrizioneStato());
		if(rpt.getEsitoPagamento() != null) {
			t.setEsito(EsitoTransazione.valueOf(rpt.getEsitoPagamento().toString()));
		}
		t.setIuv(rpt.getIuv());
		t.setModello(ModelloPagamento.valueOf(rpt.getModelloPagamento().toString()));
		t.setRpt(rpt.getXmlRpt());
		t.setRt(rpt.getXmlRt());

		if(versione.compareTo(Versione.GP_02_02_00) >=0) {
			t.setData(rpt.getDataMsgRichiesta());
		}

		try {
			t.setStato(StatoTransazione.valueOf(rpt.getStato().toString()));
		} catch (Exception e) {
			t.setStato(StatoTransazione.RPT_ACCETTATA_NODO);
		}
		for(Pagamento pagamento : rpt.getPagamenti(bd)) {
			t.getPagamento().add(toPagamento(pagamento, versione, bd));
		}
		return t;
	}

	public static it.govpay.servizi.gpprt.GpChiediListaVersamentiResponse.Versamento toVersamento(Versione versione, Versamento versamento, BasicBD bd) throws ServiceException {
		it.govpay.servizi.gpprt.GpChiediListaVersamentiResponse.Versamento v = new it.govpay.servizi.gpprt.GpChiediListaVersamentiResponse.Versamento();
		v.setCodApplicazione(versamento.getApplicazione(bd).getCodApplicazione());
		v.setCodVersamentoEnte(versamento.getCodVersamentoEnte());
		v.setDataScadenza(versamento.getDataScadenza());
		v.setImportoTotale(versamento.getImportoTotale());
		v.setStato(StatoVersamento.valueOf(versamento.getStatoVersamento().toString()));
		if(versamento.getCausaleVersamento() instanceof Versamento.CausaleSemplice)
			v.setCausale(((Versamento.CausaleSemplice) versamento.getCausaleVersamento()).getCausale());
		if(versamento.getCausaleVersamento() instanceof Versamento.CausaleSpezzoni)
			v.getSpezzoneCausale().addAll(((Versamento.CausaleSpezzoni) versamento.getCausaleVersamento()).getSpezzoni());
		if(versamento.getCausaleVersamento() instanceof Versamento.CausaleSpezzoniStrutturati) {
			Versamento.CausaleSpezzoniStrutturati c = (Versamento.CausaleSpezzoniStrutturati) versamento.getCausaleVersamento();
			for(int i = 0; i<c.getImporti().size(); i++) {
				SpezzoneCausaleStrutturata s = new SpezzoneCausaleStrutturata();
				s.setCausale(c.getSpezzoni().get(i));
				s.setImporto(c.getImporti().get(i));
				v.getSpezzoneCausaleStrutturata().add(s);
			}
		}
		if(versione.compareTo(Versione.GP_02_02_00) >=0) {
			v.setCodDominio(versamento.getUo(bd).getDominio(bd).getCodDominio());
			Iuv iuv = versamento.getIuv(bd);
			if(iuv != null) {
				IuvGenerato iuvGenerato = IuvUtils.toIuvGenerato(versamento.getApplicazione(bd), versamento.getUo(bd).getDominio(bd), iuv, versamento.getImportoTotale(), versione);
				v.setIuv(iuv.getIuv());
				v.setBarCode(iuvGenerato.getBarCode());
				v.setQrCode(iuvGenerato.getQrCode());
				if(versione.compareTo(Versione.GP_02_03_00) >=0) {
					v.setNumeroAvviso(iuvGenerato.getNumeroAvviso());
				}
			}
		}
		return v;
	}

	public static it.govpay.servizi.commons.Pagamento toPagamento(Pagamento pagamento, Versionabile.Versione versione, BasicBD bd) throws ServiceException {
		it.govpay.servizi.commons.Pagamento p = new it.govpay.servizi.commons.Pagamento();

		if(pagamento.getAllegato() != null) {
			Allegato allegato = new Allegato();
			allegato.setTesto(pagamento.getAllegato());
			allegato.setTipo(TipoAllegato.valueOf(pagamento.getTipoAllegato().toString()));
			p.setAllegato(allegato);
		}
		p.setCodSingoloVersamentoEnte(pagamento.getSingoloVersamento(bd).getCodSingoloVersamentoEnte());
		p.setCommissioniPsp(pagamento.getCommissioniPsp());
		p.setDataPagamento(pagamento.getDataPagamento());
		p.setImportoPagato(pagamento.getImportoPagato());
		p.setIur(pagamento.getIur());
		p.setCausaleRevoca(pagamento.getCausaleRevoca());
		p.setDatiEsitoRevoca(pagamento.getDatiEsitoRevoca());
		p.setDatiRevoca(pagamento.getDatiRevoca());
		p.setEsitoRevoca(pagamento.getEsitoRevoca());
		p.setImportoRevocato(pagamento.getImportoRevocato());
		if(versione.compareTo(Versione.GP_02_02_00) >= 0) {
			p.setDataAcquisizione(pagamento.getDataAcquisizione());
			p.setDataAcquisizioneRevoca(pagamento.getDataAcquisizioneRevoca());
		}
		return p;
	}

	public static Storno toStorno(Rr rr, Versionabile.Versione versione, BasicBD bd) throws ServiceException {
		Storno storno = new Storno();
		storno.setCcp(rr.getCcp());
		storno.setCodDominio(rr.getCodDominio());
		storno.setDescrizioneStato(rr.getDescrizioneStato());
		storno.setEr(rr.getXmlEr());
		storno.setIuv(rr.getIuv());
		storno.setRr(rr.getXmlRr());
		storno.setStato(StatoRevoca.fromValue(rr.getStato().toString()));
		for(Pagamento p : rr.getPagamenti(bd)) {
			storno.getPagamento().add(toPagamento(p, versione, bd));
		}
		return storno;
	}
	
	public static FlussoRendicontazione toFr(Fr frModel, List<RendicontazionePagamento> rends, Versione versione, BasicBD bd) throws ServiceException {
		
		FlussoRendicontazione fr = new FlussoRendicontazione();
		int annoFlusso = Integer.parseInt(simpleDateFormatAnno.format(frModel.getDataFlusso()));
		fr.setAnnoRiferimento(annoFlusso);
		fr.setCodBicRiversamento(frModel.getCodBicRiversamento());
		fr.setCodFlusso(frModel.getCodFlusso());
		fr.setCodPsp(frModel.getCodPsp());
		fr.setDataFlusso(frModel.getDataFlusso());
		fr.setDataRegolamento(frModel.getDataRegolamento());
		fr.setImportoTotale(frModel.getImportoTotalePagamenti());
		fr.setNumeroPagamenti(frModel.getNumeroPagamenti());
		fr.setIur(frModel.getIur());
		
		for(RendicontazionePagamento rend : rends) {
			fr.getPagamento().add(Gp21Utils.toRendicontazionePagamento(rend, versione, bd));
		}
		return fr;
	}

	public static it.govpay.servizi.commons.FlussoRendicontazione.Pagamento toRendicontazionePagamento(RendicontazionePagamento rend, Versione versione, BasicBD bd) throws ServiceException {
		FlussoRendicontazione.Pagamento p = new FlussoRendicontazione.Pagamento();
		if(rend.getSingoloVersamento() != null)
			p.setCodSingoloVersamentoEnte(rend.getSingoloVersamento().getCodSingoloVersamentoEnte());
		
		p.setImportoRendicontato(rend.getRendicontazione().getImporto().abs());
		p.setIur(rend.getRendicontazione().getIur());
		p.setEsitoRendicontazione(TipoRendicontazione.valueOf(rend.getRendicontazione().getEsito().toString()));
		p.setDataRendicontazione(rend.getRendicontazione().getData());
		if(versione.compareTo(Versione.GP_02_02_00) >= 0) {
			if(rend.getVersamento() != null)
				p.setCodApplicazione(rend.getVersamento().getApplicazione(bd).getCodApplicazione());
			p.setIuv(rend.getRendicontazione().getIuv());
			p.setCodDominio(rend.getFr().getCodDominio());
		}
		
		return p;
	}

	public static List<it.govpay.bd.model.Versamento.StatoVersamento> toStatiVersamento(List<StatoVersamento> stati) {
		if(stati == null || stati.size() == 0) return null;

		List<it.govpay.bd.model.Versamento.StatoVersamento> statiVersamento = new ArrayList<it.govpay.bd.model.Versamento.StatoVersamento>();
		for(StatoVersamento stato : stati) {
			statiVersamento.add(it.govpay.bd.model.Versamento.StatoVersamento.valueOf(stato.toString()));
		}
		return statiVersamento;
	}

	public static SortFields toFilterSort(String ordinamento) {
		if(ordinamento == null) return null;

		if(ordinamento.equals("DATA_SCADENZA_ASC"))
			return SortFields.SCADENZA_ASC;

		if(ordinamento.equals("DATA_SCADENZA_DES"))
			return SortFields.SCADENZA_DESC;

		if(ordinamento.equals("DATA_CARICAMENTO_ASC"))
			return SortFields.CARICAMENTO_ASC;

		if(ordinamento.equals("DATA_CARICAMENTO_DES"))
			return SortFields.CARICAMENTO_DESC;

		if(ordinamento.equals("DATA_AGGIORNAMENTO_ASC"))
			return SortFields.AGGIORNAMENTO_ASC;

		if(ordinamento.equals("DATA_AGGIORNAMENTO_DES"))
			return SortFields.AGGIORNAMENTO_DESC;

		return null;
	}

	public static SimpleDateFormat simpleDateFormatAnno = new SimpleDateFormat("yyyy");
	public static it.govpay.servizi.gprnd.GpChiediListaFlussiRendicontazioneResponse.FlussoRendicontazione toFr(Fr frModel, Versione versione, BasicBD bd) {
		GpChiediListaFlussiRendicontazioneResponse.FlussoRendicontazione efr = new GpChiediListaFlussiRendicontazioneResponse.FlussoRendicontazione();
		int annoFlusso = Integer.parseInt(simpleDateFormatAnno.format(frModel.getDataFlusso()));
		efr.setAnnoRiferimento(annoFlusso);
		efr.setCodBicRiversamento(frModel.getCodBicRiversamento());
		efr.setCodDominio(frModel.getCodDominio());
		efr.setCodFlusso(frModel.getCodFlusso());
		efr.setCodPsp(frModel.getCodPsp());
		efr.setDataFlusso(frModel.getDataFlusso());
		efr.setDataRegolamento(frModel.getDataRegolamento());
		efr.setIur(frModel.getIur());
		efr.setImportoTotale(frModel.getImportoTotalePagamenti());
		efr.setNumeroPagamenti(frModel.getNumeroPagamenti());
		return efr;
	}
	
}
