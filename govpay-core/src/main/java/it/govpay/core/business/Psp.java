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
package it.govpay.core.business;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.logger.beans.Property;

import it.gov.digitpa.schemas._2011.psp.CtInformativaDetail;
import it.gov.digitpa.schemas._2011.psp.CtInformativaMaster;
import it.gov.digitpa.schemas._2011.psp.InformativaPSP;
import it.gov.digitpa.schemas._2011.psp.ListaInformativePSP;
import it.gov.digitpa.schemas._2011.psp.ObjectFactory;
import it.gov.digitpa.schemas._2011.ws.paa.NodoChiediInformativaPSP;
import it.gov.digitpa.schemas._2011.ws.paa.NodoChiediInformativaPSPRisposta;
import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.PspBD;
import it.govpay.bd.anagrafica.StazioniBD;
import it.govpay.servizi.commons.EsitoOperazione;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.client.NodoClient;
import it.govpay.core.utils.client.NodoClient.Azione;
import it.govpay.bd.model.Canale;
import it.govpay.model.Intermediario;
import it.govpay.bd.model.Stazione;

public class Psp extends BasicBD {

	private static Logger log = LogManager.getLogger();

	public Psp(BasicBD basicBD) {
		super(basicBD);
	}

	public List<it.govpay.bd.model.Psp> chiediListaPsp() throws ServiceException {

		PspBD pspBD = new PspBD(this);
		List<it.govpay.bd.model.Psp> psps = pspBD.getPsp(true);
		return psps;
	}


	public String aggiornaRegistro() throws GovPayException {
		List<String> response = new ArrayList<String>();

		GpContext ctx = GpThreadLocal.get();

		log.info("Aggiornamento del Registro PSP");
		ctx.log("psp.aggiornamentoPsp");
		boolean acquisizioneOk = false;
		Throwable lastError = null;
		String transactionId = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);

			
			StazioniBD stazioniBD = new StazioniBD(this);
			List<Stazione> lstStazioni = stazioniBD.getStazioni();
			
			if(lstStazioni.size() == 0) {
				log.warn("Nessuna stazione registrata. Impossibile richiedere il catalogo dei Psp.");
				ctx.log("psp.aggiornamentoPspNoDomini");
				throw new GovPayException(EsitoOperazione.INTERNAL, "Nessuna stazione registrata. Impossibile richiedere il catalogo dei Psp.");
			}
			
			// Finche' non ricevo un catalogo di informativa, provo per tutte le stazioni.
			ListaInformativePSP informativePsp = null;

			for(Stazione stazione : lstStazioni) {

				log.info("Richiedo catalogo per la stazione " + stazione.getCodStazione());
				Intermediario intermediario = stazione.getIntermediario(this);

				transactionId = ctx.openTransaction();
				ctx.getContext().getRequest().addGenericProperty(new Property("codStazione", stazione.getCodStazione()));
				ctx.setupNodoClient(stazione.getCodStazione(), null, Azione.nodoChiediInformativaPSP);
				ctx.log("psp.aggiornamentoPspRichiesta");

				closeConnection();

				NodoChiediInformativaPSP richiesta = new NodoChiediInformativaPSP();
				richiesta.setIdentificativoIntermediarioPA(intermediario.getCodIntermediario());
				richiesta.setIdentificativoStazioneIntermediarioPA(stazione.getCodStazione());
				richiesta.setPassword(stazione.getPassword());

				try {
					NodoClient client = null;
					try { 
						client = new NodoClient(intermediario, this);

						NodoChiediInformativaPSPRisposta risposta = client.nodoChiediInformativaPSP(richiesta, intermediario.getDenominazione());

						if(risposta.getFault() != null) {
							throw new GovPayException(risposta.getFault());
						}

						DataHandler dh= risposta.getXmlInformativa();
						ByteArrayOutputStream output = new ByteArrayOutputStream();
						dh.writeTo(output);

						Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
						informativePsp = (ListaInformativePSP) jaxbUnmarshaller.unmarshal(risposta.getXmlInformativa().getDataSource().getInputStream());
					} finally {
						setupConnection(ctx.getTransactionId());
					}

					if(informativePsp == null) {
						log.warn("Catalogo dei psp non acquisito. Impossibile aggiornare il registro.");
						ctx.log("psp.aggiornamentoPspRichiestaKo", "Ricevuta Informativa PSP vuota.");
						throw new GovPayException("Ricevuta Informativa PSP vuota.", EsitoOperazione.INTERNAL, "Impossibile aggiornare la lista dei Psp.");
					}

					log.info("Ricevuto catalogo dei dati Informativi con " + informativePsp.getInformativaPSPs().size() + " informative.");
					List<it.govpay.bd.model.Psp> catalogoPsp = new ArrayList<it.govpay.bd.model.Psp>();

					// Converto ogni informativa un PSP
					for(InformativaPSP informativaPsp : informativePsp.getInformativaPSPs()) {
						it.govpay.bd.model.Psp psp = new it.govpay.bd.model.Psp();

						CtInformativaMaster informativaMaster = informativaPsp.getInformativaMaster();

						boolean isAttivo = informativaMaster.getDataInizioValidita().before(new Date());

						psp.setAbilitato(isAttivo);
						psp.setCodFlusso(informativaPsp.getIdentificativoFlusso());
						psp.setCodPsp(informativaPsp.getIdentificativoPSP());
						psp.setRagioneSociale(informativaPsp.getRagioneSociale());
						psp.setStornoGestito(informativaMaster.getStornoPagamento() == 1);
						psp.setBolloGestito(informativaMaster.getMarcaBolloDigitale() == 1);
						psp.setUrlInfo(informativaMaster.getUrlInformazioniPSP());

						for(CtInformativaDetail informativaPspDetail : informativaPsp.getListaInformativaDetail().getInformativaDetails()) {
							Canale canale = new Canale();
							//canale.setCondizioni(informativaPspDetail.getCondizioniEconomicheMassime());
							canale.setCodCanale(informativaPspDetail.getIdentificativoCanale());
							//canale.setDescrizione(informativaPspDetail.getDescrizioneServizio());
							//canale.setDisponibilita(informativaPspDetail.getDisponibilitaServizio());
							canale.setModelloPagamento(Canale.ModelloPagamento.toEnum(informativaPspDetail.getModelloPagamento()));
							canale.setPsp(psp);
							canale.setTipoVersamento(Canale.TipoVersamento.toEnum(informativaPspDetail.getTipoVersamento().name()));
							//canale.setUrlInfo(informativaPspDetail.getUrlInformazioniCanale());
							canale.setCodIntermediario(informativaPspDetail.getIdentificativoIntermediario());
							psp.getCanalis().add(canale);
						}

						catalogoPsp.add(psp);
						log.debug("Acquisita informativa [codPsp: " + psp.getCodPsp() + "]");
					}

					// Completata acquisizione del Catalogo dal Nodo dei Pagamenti.

					// Disabilito tutti i PSP e li aggiorno o inserisco in base
					// a quello che ho trovato sul catalogo.
					setAutoCommit(false);

					PspBD pspBD = new PspBD(this);
					List<it.govpay.bd.model.Psp> oldPsps = pspBD.getPsp();
					while(!oldPsps.isEmpty()) {
						it.govpay.model.Psp psp = oldPsps.remove(0);
						// Cerco il psp nel Catalogo appena ricevuto
						boolean trovato = false;
						for(int i = 0; i<catalogoPsp.size(); i++ ) {
							if(catalogoPsp.get(i).getCodPsp().equals(psp.getCodPsp())) {

								// Il psp e' nel catalogo, va aggiornato. 
								// Rimuovo la versione aggiornata dal catalogo e lo mando in update
								log.debug("Aggiornamento [codPsp: " + psp.getCodPsp() + "]");
								ctx.log("psp.aggiornamentoPspAggiornatoPSP", psp.getCodPsp(), psp.getRagioneSociale());
								response.add(psp.getRagioneSociale() + " (" + psp.getCodPsp() + ")#Acquisita versione aggiornata.");
								pspBD.updatePsp(catalogoPsp.get(i));
								catalogoPsp.remove(i);
								trovato = true;
								break;
							}
						}

						if(!trovato) {
							// Il psp non e' nel catalogo.
							// Se era attivo, lo disattivo.
							if(psp.isAbilitato()) {
								log.info("Disabilitazione [codPsp: " + psp.getCodPsp() + "]");
								ctx.log("psp.aggiornamentoPspDisabilitatoPSP", psp.getCodPsp(), psp.getRagioneSociale());
								response.add(psp.getRagioneSociale() + " (" + psp.getCodPsp() + ")#Disabilitato.");
								pspBD.disablePsp(psp.getId());
							}
						}
					}

					// I psp rimasti nel catalogo, sono nuovi e vanno aggiunti
					for(it.govpay.bd.model.Psp psp : catalogoPsp) {
						log.info("Inserimento [codPsp: " + psp.getCodPsp() + "]");
						ctx.log("psp.aggiornamentoPspInseritoPSP", psp.getCodPsp(), psp.getRagioneSociale());
						response.add(psp.getRagioneSociale() + " (" + psp.getCodPsp() + ")#Aggiunto al registro.");
						pspBD.insertPsp(psp);
					}

					commit();

					log.info("Aggiornamento Registro PSP completato.");
					ctx.log("psp.aggiornamentoPspRichiestaOk");
					acquisizioneOk = true;
					break;
				} catch (Exception e) {
					log.warn("Errore di acquisizione del Catalogo dati Informativi [Intermediario:" + intermediario.getCodIntermediario() + " Stazione:" + stazione.getCodStazione() + "]", e);
					ctx.log("psp.aggiornamentoPspRichiestaKo", e.getMessage());
					lastError = e;
					continue;
				} finally {
					ctx.closeTransaction(transactionId);
				}
			}

			if(acquisizioneOk) {
				ctx.log("psp.aggiornamentoPspOk");
				if(response.isEmpty()) {
					return "Acquisizione completata#Nessun psp acquisito.";
				} else {
					return StringUtils.join(response,"|");
				}
			} else {
				log.error("Impossibile aggiornare la lista dei Psp.", lastError);
				ctx.log("psp.aggiornamentoPspKo", lastError == null ? "[-- No exception found --]" : lastError.getMessage());
				return "Acquisizione fallita#Riscontrato errore:" + lastError.getMessage();
			}
		} catch (Throwable se) {
			rollback();
			ctx.log("psp.aggiornamentoPspKo", se.getMessage());
			log.error("Impossibile aggiornare la lista dei Psp.", se);
			throw new GovPayException(se, "Impossibile aggiornare la lista dei Psp.");
		} finally {
			ctx.closeTransaction(transactionId);
		}
	}
}
