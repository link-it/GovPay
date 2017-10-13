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
package it.govpay.web.ws;

import java.util.Base64;
import java.util.Date;

import com.google.zxing.aztec.encoder.Encoder;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.ws.nodo.EsitoPaaInviaRT;
import it.gov.digitpa.schemas._2011.ws.nodo.PaaInviaRT;
import it.gov.digitpa.schemas._2011.ws.nodo.PaaInviaRTRisposta;
import it.gov.digitpa.schemas._2011.ws.nodo.TipoInviaEsitoStornoRisposta;
import it.gov.digitpa.schemas._2011.ws.nodo.FaultBean;
import it.gov.spcoop.nodopagamentispc.servizi.pagamentitelematicirt.PagamentiTelematiciRT;
import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.core.business.GiornaleEventi;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.exceptions.NdpException;
import it.govpay.core.exceptions.NdpException.FaultPa;
import it.govpay.core.utils.GovpayConfig;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.RrUtils;
import it.govpay.core.utils.RtUtils;
import it.govpay.bd.model.Dominio;
import it.govpay.model.Evento;
import it.govpay.model.Intermediario;
import it.govpay.bd.model.Rpt;
import it.govpay.bd.model.Rr;
import it.govpay.bd.model.Stazione;
import it.govpay.model.Evento.TipoEvento;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openspcoop2.generic_project.exception.NotAuthorizedException;
import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.utils.logger.beans.Property;
import org.openspcoop2.utils.logger.beans.proxy.Actor;

@WebService(serviceName = "PagamentiTelematiciRTservice",
endpointInterface = "it.gov.spcoop.nodopagamentispc.servizi.pagamentitelematicirt.PagamentiTelematiciRT",
targetNamespace = "http://NodoPagamentiSPC.spcoop.gov.it/servizi/PagamentiTelematiciRT",
portName = "PPTPort",
wsdlLocation = "classpath:wsdl/PaPerNodo.wsdl")

@org.apache.cxf.annotations.SchemaValidation(type = SchemaValidationType.IN)

@HandlerChain(file="../../../../handler-chains/handler-chain-ndp.xml")

public class PagamentiTelematiciRTImpl implements PagamentiTelematiciRT {

	@Resource
	WebServiceContext wsCtxt;

	private static Logger log = LogManager.getLogger();

	@Override
	public TipoInviaEsitoStornoRisposta paaInviaEsitoStorno(
			String identificativoIntermediarioPA,
			String identificativoStazioneIntermediarioPA,
			String identificativoDominio,
			String identificativoUnivocoVersamento,
			String codiceContestoPagamento, byte[] er) {

		GpContext ctx = GpThreadLocal.get();

		ctx.setCorrelationId(identificativoDominio + identificativoUnivocoVersamento + codiceContestoPagamento);

		Actor from = new Actor();
		from.setName("NodoDeiPagamentiSPC");
		from.setType(GpContext.TIPO_SOGGETTO_NDP);
		ctx.getTransaction().setFrom(from);

		Actor to = new Actor();
		to.setName(identificativoStazioneIntermediarioPA);
		from.setType(GpContext.TIPO_SOGGETTO_STAZIONE);
		ctx.getTransaction().setTo(to);

		ctx.getContext().getRequest().addGenericProperty(new Property("ccp", codiceContestoPagamento));
		ctx.getContext().getRequest().addGenericProperty(new Property("codDominio", identificativoDominio));
		ctx.getContext().getRequest().addGenericProperty(new Property("iuv", identificativoUnivocoVersamento));
		ctx.log("er.ricezione");

		log.info("Ricevuta richiesta di acquisizione ER [" + identificativoDominio + "][" + identificativoUnivocoVersamento + "][" + codiceContestoPagamento + "]");

		TipoInviaEsitoStornoRisposta response = new TipoInviaEsitoStornoRisposta();

		BasicBD bd = null;

		Evento evento = new Evento();
		evento.setCodStazione(identificativoStazioneIntermediarioPA);
		evento.setCodDominio(identificativoDominio);
		evento.setIuv(identificativoUnivocoVersamento);
		evento.setCcp(codiceContestoPagamento);
		evento.setTipoEvento(TipoEvento.paaInviaEsitoStorno);
		evento.setFruitore("NodoDeiPagamentiSPC");

		try {
			bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());

			String principal = getPrincipal();
			if(GovpayConfig.getInstance().isPddAuthEnable() && principal == null) {
				ctx.log("er.erroreNoAutorizzazione");
				throw new NotAuthorizedException("Autorizzazione fallita: principal non fornito");
			}

			Intermediario intermediario = null;
			try {
				intermediario = AnagraficaManager.getIntermediario(bd, identificativoIntermediarioPA);

				// Controllo autorizzazione
				if(GovpayConfig.getInstance().isPddAuthEnable() && !principal.equals(intermediario.getConnettorePdd().getPrincipal())){
					ctx.log("er.erroreAutorizzazione", principal);
					throw new NotAuthorizedException("Autorizzazione fallita: principal fornito non corrisponde all'intermediario " + identificativoIntermediarioPA);
				}

				evento.setErogatore(intermediario.getDenominazione());
			} catch (NotFoundException e) {
				throw new NdpException(FaultPa.PAA_ID_INTERMEDIARIO_ERRATO, identificativoDominio);
			}


			Dominio dominio = null;
			try {
				dominio = AnagraficaManager.getDominio(bd, identificativoDominio);
			} catch (NotFoundException e) {
				throw new NdpException(FaultPa.PAA_ID_DOMINIO_ERRATO, identificativoDominio);
			}

			Stazione stazione = null;
			try {
				stazione = AnagraficaManager.getStazione(bd, identificativoStazioneIntermediarioPA);
			} catch (NotFoundException e) {
				throw new NdpException(FaultPa.PAA_STAZIONE_INT_ERRATA, identificativoDominio);
			}

			if(stazione.getIdIntermediario() != intermediario.getId()) {
				throw new NdpException(FaultPa.PAA_ID_INTERMEDIARIO_ERRATO, identificativoDominio);
			}

			if(dominio.getIdStazione() != stazione.getId()) {
				throw new NdpException(FaultPa.PAA_STAZIONE_INT_ERRATA, identificativoDominio);
			}

			Rr rr = RrUtils.acquisisciEr(identificativoDominio, identificativoUnivocoVersamento, codiceContestoPagamento, er, bd);
			evento.setCodCanale(rr.getRpt(bd).getCanale(bd).getCodCanale());
			evento.setTipoVersamento(rr.getRpt(bd).getCanale(bd).getTipoVersamento());
			response.setEsito("OK");
			ctx.log("er.ricezioneOk");
		} catch (NdpException e) {
			if(bd != null) bd.rollback();
			response = buildRisposta(e, response);
			String faultDescription = response.getFault().getDescription() == null ? "<Nessuna descrizione>" : response.getFault().getDescription(); 
			ctx.log("er.ricezioneKo", response.getFault().getFaultCode(), response.getFault().getFaultString(), faultDescription);
		} catch (Exception e) {
			if(bd != null) bd.rollback();
			response = buildRisposta(new NdpException(FaultPa.PAA_SYSTEM_ERROR, identificativoDominio, e.getMessage(), e), response);
			String faultDescription = response.getFault().getDescription() == null ? "<Nessuna descrizione>" : response.getFault().getDescription(); 
			ctx.log("er.ricezioneKo", response.getFault().getFaultCode(), response.getFault().getFaultString(), faultDescription);
		} finally {
			try{
				if(bd != null) {
					bd.setAutoCommit(true);
					GiornaleEventi ge = new GiornaleEventi(bd);
					evento.setEsito(response.getEsito());
					evento.setDataRisposta(new Date());
					ge.registraEvento(evento);
				}

				if(ctx != null) {
					ctx.setResult(response.getFault() == null ? null : response.getFault().getFaultCode());
					ctx.log();
				}
			}catch(Exception e){
				log.error(e,e);
			}

			if(bd != null) bd.closeConnection();
		}
		return response;
	}

	@Override
	public PaaInviaRTRisposta paaInviaRT(PaaInviaRT bodyrichiesta, IntestazionePPT header) {

		String ccp = header.getCodiceContestoPagamento();
		String codDominio = header.getIdentificativoDominio();
		String iuv = header.getIdentificativoUnivocoVersamento();

		GpContext ctx = GpThreadLocal.get();

		ctx.setCorrelationId(codDominio + iuv + ccp);

		Actor from = new Actor();
		from.setName("NodoDeiPagamentiSPC");
		from.setType(GpContext.TIPO_SOGGETTO_NDP);
		ctx.getTransaction().setFrom(from);

		Actor to = new Actor();
		to.setName(header.getIdentificativoStazioneIntermediarioPA());
		from.setType(GpContext.TIPO_SOGGETTO_STAZIONE);
		ctx.getTransaction().setTo(to);

		ctx.getContext().getRequest().addGenericProperty(new Property("ccp", ccp));
		ctx.getContext().getRequest().addGenericProperty(new Property("codDominio", codDominio));
		ctx.getContext().getRequest().addGenericProperty(new Property("iuv", iuv));
		ctx.log("pagamento.ricezioneRt");
		
		log.info("Ricevuta richiesta di acquisizione RT [" + codDominio + "][" + iuv + "][" + ccp + "]");
		PaaInviaRTRisposta response = new PaaInviaRTRisposta();

		BasicBD bd = null;

		Evento evento = new Evento();
		evento.setCodStazione(header.getIdentificativoStazioneIntermediarioPA());
		evento.setCodDominio(codDominio);
		evento.setIuv(iuv);
		evento.setCcp(ccp);
		evento.setTipoEvento(TipoEvento.paaInviaRT);
		evento.setFruitore("NodoDeiPagamentiSPC");

		try {
			bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());

			String principal = getPrincipal();
			if(GovpayConfig.getInstance().isPddAuthEnable() && principal == null) {
				ctx.log("rt.erroreNoAutorizzazione");
				throw new NotAuthorizedException("Autorizzazione fallita: principal non fornito");
			}

			Intermediario intermediario = null;
			try {
				intermediario = AnagraficaManager.getIntermediario(bd, header.getIdentificativoIntermediarioPA());

				// Controllo autorizzazione
				if(GovpayConfig.getInstance().isPddAuthEnable() && !principal.equals(intermediario.getConnettorePdd().getPrincipal())){
					ctx.log("rt.erroreAutorizzazione", principal);
					throw new NotAuthorizedException("Autorizzazione fallita: principal fornito (" + principal + ") non corrisponde all'intermediario " + header.getIdentificativoIntermediarioPA() + ". Atteso [" + intermediario.getConnettorePdd().getPrincipal() + "]");
				}

				evento.setErogatore(intermediario.getDenominazione());
			} catch (NotFoundException e) {
				throw new NdpException(FaultPa.PAA_ID_INTERMEDIARIO_ERRATO, codDominio);
			}

			Dominio dominio = null;
			try {
				dominio = AnagraficaManager.getDominio(bd, codDominio);
			} catch (NotFoundException e) {
				throw new NdpException(FaultPa.PAA_ID_DOMINIO_ERRATO, codDominio);
			}

			Stazione stazione = null;
			try {
				stazione = AnagraficaManager.getStazione(bd, header.getIdentificativoStazioneIntermediarioPA());
			} catch (NotFoundException e) {
				throw new NdpException(FaultPa.PAA_STAZIONE_INT_ERRATA, codDominio);
			}

			if(stazione.getIdIntermediario() != intermediario.getId()) {
				throw new NdpException(FaultPa.PAA_ID_INTERMEDIARIO_ERRATO, codDominio);
			}

			if(dominio.getIdStazione() != stazione.getId()) {
				throw new NdpException(FaultPa.PAA_STAZIONE_INT_ERRATA, codDominio);
			}

			Rpt rpt = RtUtils.acquisisciRT(codDominio, iuv, ccp, bodyrichiesta.getTipoFirma(), bodyrichiesta.getRt(), bd);
			
			ctx.getContext().getResponse().addGenericProperty(new Property("esitoPagamento", rpt.getEsitoPagamento().toString()));
			ctx.log("pagamento.acquisizioneRtOk");
			
			evento.setCodCanale(rpt.getCanale(bd).getCodCanale());
			evento.setTipoVersamento(rpt.getCanale(bd).getTipoVersamento());

			EsitoPaaInviaRT esito = new EsitoPaaInviaRT();
			esito.setEsito("OK");
			response.setPaaInviaRTRisposta(esito);
			ctx.log("rt.ricezioneOk");
		} catch (NdpException e) {
			if(bd != null) bd.rollback();
			response = buildRisposta(e, response);
			String faultDescription = response.getPaaInviaRTRisposta().getFault().getDescription() == null ? "<Nessuna descrizione>" : response.getPaaInviaRTRisposta().getFault().getDescription(); 
			ctx.log("rt.ricezioneKo", response.getPaaInviaRTRisposta().getFault().getFaultCode(), response.getPaaInviaRTRisposta().getFault().getFaultString(), faultDescription);
		} catch (Exception e) {
			if(bd != null) bd.rollback();
			response = buildRisposta(new NdpException(FaultPa.PAA_SYSTEM_ERROR, codDominio, e.getMessage(), e), response);
			String faultDescription = response.getPaaInviaRTRisposta().getFault().getDescription() == null ? "<Nessuna descrizione>" : response.getPaaInviaRTRisposta().getFault().getDescription(); 
			ctx.log("rt.ricezioneKo", response.getPaaInviaRTRisposta().getFault().getFaultCode(), response.getPaaInviaRTRisposta().getFault().getFaultString(), faultDescription);
		} finally {
			try{
				if(bd != null) {
					bd.setAutoCommit(true);
					GiornaleEventi ge = new GiornaleEventi(bd);
					evento.setEsito(response.getPaaInviaRTRisposta().getEsito());
					evento.setDataRisposta(new Date());
					ge.registraEvento(evento);
				}


				if(ctx != null) {
					ctx.setResult(response.getPaaInviaRTRisposta().getFault() == null ? null : response.getPaaInviaRTRisposta().getFault().getFaultCode());
					ctx.log();
				}

			}catch(Exception e){
				log.error(e,e);
			}

			if(bd != null) bd.closeConnection();
		}
		return response;
	}

	private <T> T buildRisposta(NdpException e, T r) {
		if(r instanceof PaaInviaRTRisposta) {
			if(e.getFault().equals(FaultPa.PAA_SYSTEM_ERROR))
				log.error("Rifiutata RT con Fault " + e.getFault().toString() + ( e.getDescrizione() != null ? (": " + e.getDescrizione()) : ""), e);
			else
				log.warn("Rifiutata RT con Fault " + e.getFault().toString() + ( e.getDescrizione() != null ? (": " + e.getDescrizione()) : ""));

			PaaInviaRTRisposta risposta = (PaaInviaRTRisposta) r;
			EsitoPaaInviaRT esito = new EsitoPaaInviaRT();
			esito.setEsito("KO");
			FaultBean fault = new FaultBean();
			fault.setId(e.getCodDominio());
			fault.setFaultCode(e.getFault().toString());
			fault.setDescription(e.getDescrizione());
			fault.setFaultString(e.getFault().getFaultString());
			esito.setFault(fault);
			risposta.setPaaInviaRTRisposta(esito);
		}

		if(r instanceof TipoInviaEsitoStornoRisposta) {
			if(e.getFault().equals(FaultPa.PAA_SYSTEM_ERROR))
				log.error("Rifiutata ER con Fault " + e.getFault().toString() + ( e.getDescrizione() != null ? (": " + e.getDescrizione()) : ""), e);
			else
				log.warn("Rifiutata ER con Fault " + e.getFault().toString() + ( e.getDescrizione() != null ? (": " + e.getDescrizione()) : ""));

			TipoInviaEsitoStornoRisposta risposta = (TipoInviaEsitoStornoRisposta) r;
			risposta.setEsito("KO");
			FaultBean fault = new FaultBean();
			fault.setId(e.getCodDominio());
			fault.setFaultCode(e.getFault().toString());
			fault.setDescription(e.getDescrizione());
			fault.setFaultString(e.getFault().getFaultString());
			risposta.setFault(fault);
		}

		return r;
	}

	private String getPrincipal() throws GovPayException {
		if(wsCtxt.getUserPrincipal() == null) {
			return null;
		}

		return wsCtxt.getUserPrincipal().getName();
	}
}
