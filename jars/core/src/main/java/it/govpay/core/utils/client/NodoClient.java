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
package it.govpay.core.utils.client;

import javax.xml.bind.JAXBElement;

import org.openspcoop2.utils.LoggerWrapperFactory;
import org.slf4j.Logger;

import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import gov.telematici.pagamenti.ws.rpt.NodoChiediCopiaRT;
import gov.telematici.pagamenti.ws.rpt.NodoChiediCopiaRTRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoChiediElencoFlussiRendicontazione;
import gov.telematici.pagamenti.ws.rpt.NodoChiediElencoFlussiRendicontazioneRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoChiediFlussoRendicontazione;
import gov.telematici.pagamenti.ws.rpt.NodoChiediFlussoRendicontazioneRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoChiediListaPendentiRPT;
import gov.telematici.pagamenti.ws.rpt.NodoChiediListaPendentiRPTRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoChiediStatoRPT;
import gov.telematici.pagamenti.ws.rpt.NodoChiediStatoRPTRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.rpt.NodoInviaCarrelloRPTRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoInviaRPT;
import gov.telematici.pagamenti.ws.rpt.NodoInviaRPTRisposta;
import gov.telematici.pagamenti.ws.rpt.NodoInviaRichiestaStorno;
import gov.telematici.pagamenti.ws.rpt.NodoInviaRichiestaStornoRisposta;
import gov.telematici.pagamenti.ws.rpt.ObjectFactory;
import gov.telematici.pagamenti.ws.rpt.Risposta;
import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.anagrafica.DominiBD;
import it.govpay.bd.anagrafica.StazioniBD;
import it.govpay.bd.model.Dominio;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.model.Intermediario;
import it.govpay.model.Rpt;
import it.govpay.model.Stazione;

public class NodoClient extends BasicClient {
	
	

	public enum Azione {
		nodoInviaRPT, nodoInviaCarrelloRPT, nodoChiediStatoRPT, nodoChiediCopiaRT, nodoChiediListaPendentiRPT, nodoInviaRichiestaStorno, nodoChiediElencoFlussiRendicontazione, nodoChiediFlussoRendicontazione
	}

	private static ObjectFactory objectFactory;
	private boolean isAzioneInUrl;
	private static Logger log = LoggerWrapperFactory.getLogger(NodoClient.class);
	private String azione, dominio, stazione, errore, faultCode;
	private BasicBD bd;

	public NodoClient(Intermediario intermediario, BasicBD bd) throws ClientException {
		super(intermediario);
		this.bd = bd;
		if(objectFactory == null || log == null ){
			objectFactory = new ObjectFactory();
		}
		this.isAzioneInUrl = intermediario.getConnettorePdd().isAzioneInUrl();
	}

	public Risposta send(String azione, JAXBElement<?> body, Object header) throws GovPayException, ClientException {
		this.azione = azione;
		String urlString = this.url.toExternalForm();
		if(this.isAzioneInUrl) {
			if(!urlString.endsWith("/")) urlString = urlString.concat("/");
		} 
		GpThreadLocal.get().getTransaction().getServer().setEndpoint(urlString);
		GpThreadLocal.get().log("ndp_client.invioRichiesta");
		
		try {
			byte[] response = super.sendSoap(azione, body, header, this.isAzioneInUrl);
			if(response == null) {
				throw new ClientException("Il Nodo dei Pagamenti ha ritornato un messaggio vuoto.");
			}
			JAXBElement<?> jaxbElement = SOAPUtils.toJaxb(response, null);
			Risposta r = (Risposta) jaxbElement.getValue();
			if(r.getFault() != null) {
				this.faultCode = r.getFault().getFaultCode() != null ? r.getFault().getFaultCode() : "<Fault Code vuoto>";
				String faultString = r.getFault().getFaultString() != null ? r.getFault().getFaultString() : "<Fault String vuoto>";
				String faultDescription = r.getFault().getDescription() != null ? r.getFault().getDescription() : "<Fault Description vuoto>";
				this.errore = "Errore applicativo " + this.faultCode + ": " + faultString;
				GpThreadLocal.get().log("ndp_client.invioRichiestaFault", this.faultCode, faultString, faultDescription);
			} else {
				GpThreadLocal.get().log("ndp_client.invioRichiestaOk");
			}
			return r;
		} catch (ClientException e) {
			this.errore = "Errore rete: " + e.getMessage();
			GpThreadLocal.get().log("ndp_client.invioRichiestaKo", e.getMessage());
			throw e;
		} catch (Exception e) {
			this.errore = "Errore interno: " + e.getMessage();
			GpThreadLocal.get().log("ndp_client.invioRichiestaKo", "Errore interno");
			throw new ClientException("Messaggio di risposta dal Nodo dei Pagamenti non valido", e);
		} finally {
			this.updateStato();
		}
		
	}
	
	public NodoInviaRPTRisposta nodoInviaRPT(Intermediario intermediario, Stazione stazione, Rpt rpt, NodoInviaRPT inviaRPT) throws GovPayException, ClientException {
		this.stazione = stazione.getCodStazione();
		this.dominio = rpt.getCodDominio();
		
		IntestazionePPT intestazione = new IntestazionePPT();
		intestazione.setCodiceContestoPagamento(rpt.getCcp());
		intestazione.setIdentificativoDominio(rpt.getCodDominio());
		intestazione.setIdentificativoIntermediarioPA(intermediario.getCodIntermediario());
		intestazione.setIdentificativoStazioneIntermediarioPA(stazione.getCodStazione());
		intestazione.setIdentificativoUnivocoVersamento(rpt.getIuv());
		
		Risposta response = send(Azione.nodoInviaRPT.toString(), objectFactory.createNodoInviaRPT(inviaRPT), intestazione);
		return (NodoInviaRPTRisposta) response;
	}

	public NodoInviaCarrelloRPTRisposta nodoInviaCarrelloRPT(Intermediario intermediario, Stazione stazione, NodoInviaCarrelloRPT inviaCarrelloRPT, String codCarrello) throws GovPayException, ClientException {
		IntestazioneCarrelloPPT intestazione = new IntestazioneCarrelloPPT();
		intestazione.setIdentificativoIntermediarioPA(intermediario.getCodIntermediario());
		intestazione.setIdentificativoStazioneIntermediarioPA(stazione.getCodStazione());
		intestazione.setIdentificativoCarrello(codCarrello);
		Risposta response = this.send(Azione.nodoInviaCarrelloRPT.toString(), objectFactory.createNodoInviaCarrelloRPT(inviaCarrelloRPT), intestazione);
		return (NodoInviaCarrelloRPTRisposta) response;
	}

	public NodoChiediStatoRPTRisposta nodoChiediStatoRpt(NodoChiediStatoRPT nodoChiediStatoRPT, String nomeSoggetto) throws GovPayException, ClientException {
		this.stazione = nodoChiediStatoRPT.getIdentificativoStazioneIntermediarioPA();
		this.dominio = nodoChiediStatoRPT.getIdentificativoDominio();
		Risposta response = this.send(Azione.nodoChiediStatoRPT.toString(), objectFactory.createNodoChiediStatoRPT(nodoChiediStatoRPT), null);
		return (NodoChiediStatoRPTRisposta) response;
	}

	public NodoChiediCopiaRTRisposta nodoChiediCopiaRT(NodoChiediCopiaRT nodoChiediCopiaRT, String nomeSoggetto) throws GovPayException, ClientException {
		this.stazione = nodoChiediCopiaRT.getIdentificativoStazioneIntermediarioPA();
		this.dominio = nodoChiediCopiaRT.getIdentificativoDominio();
		Risposta response = this.send(Azione.nodoChiediCopiaRT.toString(), objectFactory.createNodoChiediCopiaRT(nodoChiediCopiaRT), null);
		return (NodoChiediCopiaRTRisposta) response;
	}

	public NodoChiediListaPendentiRPTRisposta nodoChiediListaPendentiRPT(NodoChiediListaPendentiRPT nodoChiediListaPendentiRPT, String nomeSoggetto) throws GovPayException, ClientException {
		this.stazione = nodoChiediListaPendentiRPT.getIdentificativoStazioneIntermediarioPA();
		this.dominio = nodoChiediListaPendentiRPT.getIdentificativoDominio();
		Risposta response = this.send(Azione.nodoChiediListaPendentiRPT.toString(), objectFactory.createNodoChiediListaPendentiRPT(nodoChiediListaPendentiRPT), null);
		return (NodoChiediListaPendentiRPTRisposta) response;
	}

	public NodoInviaRichiestaStornoRisposta nodoInviaRichiestaStorno(NodoInviaRichiestaStorno nodoInviaRichiestaStorno) throws GovPayException, ClientException {
		this.stazione = nodoInviaRichiestaStorno.getIdentificativoStazioneIntermediarioPA();
		this.dominio = nodoInviaRichiestaStorno.getIdentificativoDominio();
		Risposta response = this.send(Azione.nodoInviaRichiestaStorno.toString(), objectFactory.createNodoInviaRichiestaStorno(nodoInviaRichiestaStorno), null);
		return (NodoInviaRichiestaStornoRisposta) response;
	}

	public NodoChiediElencoFlussiRendicontazioneRisposta nodoChiediElencoFlussiRendicontazione(NodoChiediElencoFlussiRendicontazione nodoChiediElencoFlussiRendicontazione, String nomeSoggetto) throws GovPayException, ClientException {
		this.stazione = nodoChiediElencoFlussiRendicontazione.getIdentificativoStazioneIntermediarioPA();
		this.dominio = nodoChiediElencoFlussiRendicontazione.getIdentificativoDominio();
		Risposta response = this.send(Azione.nodoChiediElencoFlussiRendicontazione.toString(), objectFactory.createNodoChiediElencoFlussiRendicontazione(nodoChiediElencoFlussiRendicontazione), null);
		return (NodoChiediElencoFlussiRendicontazioneRisposta) response;
	}

	public NodoChiediFlussoRendicontazioneRisposta nodoChiediFlussoRendicontazione(NodoChiediFlussoRendicontazione nodoChiediFlussoRendicontazione, String nomeSoggetto) throws GovPayException, ClientException {
		this.stazione = nodoChiediFlussoRendicontazione.getIdentificativoStazioneIntermediarioPA();
		this.dominio = nodoChiediFlussoRendicontazione.getIdentificativoDominio();
		Risposta response = this.send(Azione.nodoChiediFlussoRendicontazione.toString(), objectFactory.createNodoChiediFlussoRendicontazione(nodoChiediFlussoRendicontazione), null);
		return (NodoChiediFlussoRendicontazioneRisposta) response;
	}

	private void updateStato() {
		boolean wasClosed = false;
		boolean wasNull = false;
		try {
			
			if(this.bd == null) {
				wasNull = true;
				this.bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
			} else {
				wasClosed = this.bd.isClosed();
				if(wasClosed) this.bd.setupConnection("--");
			}
		
			if(this.dominio != null) {
				DominiBD dominiBD = new DominiBD(this.bd);
				Dominio dominio = AnagraficaManager.getDominio(this.bd, this.dominio);
				if(this.faultCode == null || !this.faultCode.startsWith("PPT")) {
					dominiBD.setStatoNdp(dominio.getId(), 0, this.azione, null);
				} else {
					dominiBD.setStatoNdp(dominio.getId(), 1, this.azione, this.errore);
				}
			}
			
			if(this.stazione != null) {
				StazioniBD stazioniBD = new StazioniBD(this.bd);
				Stazione stazione = AnagraficaManager.getStazione(this.bd, this.stazione);
				if(this.faultCode == null || !this.faultCode.startsWith("PPT")) {
					stazioniBD.setStatoNdp(stazione.getId(), 0, this.azione, null);
				} else if(this.dominio == null) { // Aggiorno in errore solo se e' un'operazione di stazione
					stazioniBD.setStatoNdp(stazione.getId(), 1, this.azione, this.errore);
				}
			}
		} catch (Exception e ) {
			LoggerWrapperFactory.getLogger(NodoClient.class).error("Fallito aggiornamento dello stato ndp Dominio/Stazione", e);
		} finally {
			if(wasNull || wasClosed) this.bd.closeConnection();
		}
		
	}
}