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
package it.govpay.core.dao.anagrafica;

import java.util.Set;

import org.openspcoop2.generic_project.exception.MultipleResultException;
import org.openspcoop2.generic_project.exception.ServiceException;

import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.anagrafica.DominiBD;
import it.govpay.bd.anagrafica.IbanAccreditoBD;
import it.govpay.bd.anagrafica.StazioniBD;
import it.govpay.bd.anagrafica.TributiBD;
import it.govpay.bd.anagrafica.UnitaOperativeBD;
import it.govpay.bd.anagrafica.filters.DominioFilter;
import it.govpay.bd.anagrafica.filters.IbanAccreditoFilter;
import it.govpay.bd.anagrafica.filters.TributoFilter;
import it.govpay.bd.anagrafica.filters.UnitaOperativaFilter;
import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.Stazione;
import it.govpay.core.dao.anagrafica.dto.FindDominiDTO;
import it.govpay.core.dao.anagrafica.dto.FindDominiDTOResponse;
import it.govpay.core.dao.anagrafica.dto.FindIbanDTO;
import it.govpay.core.dao.anagrafica.dto.FindIbanDTOResponse;
import it.govpay.core.dao.anagrafica.dto.FindTributiDTO;
import it.govpay.core.dao.anagrafica.dto.FindTributiDTOResponse;
import it.govpay.core.dao.anagrafica.dto.FindUnitaOperativeDTO;
import it.govpay.core.dao.anagrafica.dto.FindUnitaOperativeDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetDominioDTO;
import it.govpay.core.dao.anagrafica.dto.GetDominioDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetIbanDTO;
import it.govpay.core.dao.anagrafica.dto.GetIbanDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetTributoDTO;
import it.govpay.core.dao.anagrafica.dto.GetTributoDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetUnitaOperativaDTO;
import it.govpay.core.dao.anagrafica.dto.GetUnitaOperativaDTOResponse;
import it.govpay.core.dao.anagrafica.dto.PutDominioDTO;
import it.govpay.core.dao.anagrafica.dto.PutDominioDTOResponse;
import it.govpay.core.dao.anagrafica.exception.DominioNonTrovatoException;
import it.govpay.core.dao.anagrafica.exception.StazioneNonTrovataException;
import it.govpay.core.exceptions.NotAuthorizedException;
import it.govpay.core.exceptions.NotFoundException;
import it.govpay.core.utils.AclEngine;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.model.Acl.Servizio;

public class DominiDAO {
	
	public PutDominioDTOResponse createOrUpdate(PutDominioDTO putDominioDTO) throws ServiceException,DominioNonTrovatoException,StazioneNonTrovataException{
		PutDominioDTOResponse dominioDTOResponse = new PutDominioDTOResponse();
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			// stazione
			try {
				Stazione stazione = AnagraficaManager.getStazione(bd, putDominioDTO.getCodStazione());
				putDominioDTO.getDominio().setIdStazione(stazione.getId()); 
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new StazioneNonTrovataException(e.getMessage());
			} 
			
			DominiBD dominiBD = new DominiBD(bd);
			DominioFilter filter = dominiBD.newFilter(false);
			filter.setCodDominio(putDominioDTO.getIdDominio());
			
			// flag creazione o update
			boolean isCreate = dominiBD.count(filter) == 0;
			dominioDTOResponse.setCreated(isCreate);
			if(isCreate) {
				dominiBD.insertDominio(putDominioDTO.getDominio());
			} else {
				dominiBD.updateDominio(putDominioDTO.getDominio());
			}
		} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
			throw new DominioNonTrovatoException(e.getMessage());
		} finally {
			bd.closeConnection();
		}
		return dominioDTOResponse;
	}

	public FindDominiDTOResponse findDomini(FindDominiDTO listaDominiDTO) throws NotAuthorizedException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<Long> domini = AclEngine.getIdDominiAutorizzati(listaDominiDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && domini.size() == 0) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per alcun dominio");
			}
			
			DominiBD dominiBD = new DominiBD(bd);
			DominioFilter filter = null;
			if(listaDominiDTO.isSimpleSearch()) {
				filter = dominiBD.newFilter(true);
				filter.setSimpleSearchString(listaDominiDTO.getSimpleSearch());
			} else {
				filter = dominiBD.newFilter(false);
				filter.setCodStazione(listaDominiDTO.getCodStazione());
				filter.setCodDominio(listaDominiDTO.getCodDominio());
				filter.setRagioneSociale(listaDominiDTO.getRagioneSociale());
				filter.setAbilitato(listaDominiDTO.getAbilitato());
			}
			filter.setIdDomini(domini);
			filter.setOffset(listaDominiDTO.getOffset());
			filter.setLimit(listaDominiDTO.getLimit());
			filter.getFilterSortList().addAll(listaDominiDTO.getFieldSortList());
			
			return new FindDominiDTOResponse(dominiBD.count(filter), dominiBD.findAll(filter));
			
		} finally {
			bd.closeConnection();
		}
	}
	
	public GetDominioDTOResponse getDominio(GetDominioDTO getDominioDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<String> domini = AclEngine.getDominiAutorizzati(getDominioDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && !domini.contains(getDominioDTO.getCodDominio())) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per il dominio " + getDominioDTO.getCodDominio());
			}
			
			GetDominioDTOResponse response = new GetDominioDTOResponse(AnagraficaManager.getDominio(bd, getDominioDTO.getCodDominio()));
			return response;
		} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
			throw new NotFoundException("Dominio " + getDominioDTO.getCodDominio() + " non censito in Anagrafica");
		} finally {
			bd.closeConnection();
		}
	}
	
	public FindUnitaOperativeDTOResponse findUnitaOperative(FindUnitaOperativeDTO findUnitaOperativeDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<Long> domini = AclEngine.getIdDominiAutorizzati(findUnitaOperativeDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && domini.size() == 0) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per alcun dominio");
			}
			
			UnitaOperativeBD unitaOperativeBD = new UnitaOperativeBD(bd);
			UnitaOperativaFilter filter = null;
			if(findUnitaOperativeDTO.isSimpleSearch()) {
				filter = unitaOperativeBD.newFilter(true);
				filter.setSimpleSearchString(findUnitaOperativeDTO.getSimpleSearch());
			} else {
				filter = unitaOperativeBD.newFilter(false);
				filter.setCodIdentificativo(findUnitaOperativeDTO.getCodIdentificativo());
				filter.setRagioneSociale(findUnitaOperativeDTO.getRagioneSociale());
				filter.setSearchAbilitato(findUnitaOperativeDTO.getAbilitato());
			}
			try {
				filter.setDominioFilter(AnagraficaManager.getDominio(bd, findUnitaOperativeDTO.getCodDominio()).getId());
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new NotFoundException("Dominio " + findUnitaOperativeDTO.getCodDominio() + " non censito in Anagrafica");
			}
			filter.setOffset(findUnitaOperativeDTO.getOffset());
			filter.setLimit(findUnitaOperativeDTO.getLimit());
			filter.getFilterSortList().addAll(findUnitaOperativeDTO.getFieldSortList());
			filter.setExcludeEC(true);
			
			return new FindUnitaOperativeDTOResponse(unitaOperativeBD.count(filter), unitaOperativeBD.findAll(filter));
		} finally {
			bd.closeConnection();
		}
	}
	
	public GetUnitaOperativaDTOResponse getUnitaOperativa(GetUnitaOperativaDTO getUnitaOperativaDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<String> domini = AclEngine.getDominiAutorizzati(getUnitaOperativaDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && !domini.contains(getUnitaOperativaDTO.getCodDominio())) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per il dominio " + getUnitaOperativaDTO.getCodDominio());
			}
			
			Dominio dominio = null;
			try {
				dominio = AnagraficaManager.getDominio(bd, getUnitaOperativaDTO.getCodDominio());
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new NotFoundException("Dominio " + getUnitaOperativaDTO.getCodDominio() + " non censito in Anagrafica");
			}
			
			return new GetUnitaOperativaDTOResponse(AnagraficaManager.getUnitaOperativaByCodUnivocoUo(bd, dominio.getId(), getUnitaOperativaDTO.getCodUnivocoUnitaOperativa()));
		} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
			throw new NotFoundException("Unita Operativa " + getUnitaOperativaDTO.getCodUnivocoUnitaOperativa() + " non censito in Anagrafica per il dominio " + getUnitaOperativaDTO.getCodDominio());
		} finally {
			bd.closeConnection();
		}
	}
	
	public FindIbanDTOResponse findIban(FindIbanDTO findIbanDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<Long> domini = AclEngine.getIdDominiAutorizzati(findIbanDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && domini.size() == 0) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per alcun dominio");
			}
			
			IbanAccreditoBD ibanAccreditoBD = new IbanAccreditoBD(bd);
			IbanAccreditoFilter filter = null;
			if(findIbanDTO.isSimpleSearch()) {
				filter = ibanAccreditoBD.newFilter(true);
				filter.setSimpleSearchString(findIbanDTO.getSimpleSearch());
			} else {
				filter = ibanAccreditoBD.newFilter(false);
				filter.setAbilitato(findIbanDTO.getAbilitato());
				filter.setCodIbanAccredito(findIbanDTO.getIban());
			}
			try {
				filter.setIdDominio(AnagraficaManager.getDominio(bd, findIbanDTO.getCodDominio()).getId());
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new NotFoundException("Dominio " + findIbanDTO.getCodDominio() + " non censito in Anagrafica");
			}
			filter.setOffset(findIbanDTO.getOffset());
			filter.setLimit(findIbanDTO.getLimit());
			filter.getFilterSortList().addAll(findIbanDTO.getFieldSortList());
			
			return new FindIbanDTOResponse(ibanAccreditoBD.count(filter), ibanAccreditoBD.findAll(filter));
		} finally {
			bd.closeConnection();
		}
	}
	
	public GetIbanDTOResponse getIban(GetIbanDTO getIbanDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<String> domini = AclEngine.getDominiAutorizzati(getIbanDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && !domini.contains(getIbanDTO.getCodDominio())) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per il dominio " + getIbanDTO.getCodDominio());
			}
			
			Dominio dominio = null;
			try {
				dominio = AnagraficaManager.getDominio(bd, getIbanDTO.getCodDominio());
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new NotFoundException("Dominio " + getIbanDTO.getCodDominio() + " non censito in Anagrafica");
			}
			GetIbanDTOResponse response = new GetIbanDTOResponse(AnagraficaManager.getIbanAccredito(bd, dominio.getId(), getIbanDTO.getCodIbanAccredito()));
			return response;
		} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
			throw new NotFoundException("Iban di accredito " + getIbanDTO.getCodIbanAccredito() + " non censito in Anagrafica per il dominio " + getIbanDTO.getCodDominio());
		} finally {
			bd.closeConnection();
		}
	}
	
	public FindTributiDTOResponse findTributi(FindTributiDTO findTributiDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<Long> domini = AclEngine.getIdDominiAutorizzati(findTributiDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && domini.size() == 0) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per alcun dominio");
			}
			
			TributiBD ibanAccreditoBD = new TributiBD(bd);
			TributoFilter filter = null;
			if(findTributiDTO.isSimpleSearch()) {
				filter = ibanAccreditoBD.newFilter(true);
				filter.setSimpleSearchString(findTributiDTO.getSimpleSearch());
			} else {
				filter = ibanAccreditoBD.newFilter(false);
				filter.setCodTributo(findTributiDTO.getCodTributo());
				filter.setDescrizione(findTributiDTO.getDescrizione());
				filter.setSearchAbilitato(findTributiDTO.getAbilitato());
			}
			try {
				filter.setIdDominio(AnagraficaManager.getDominio(bd, findTributiDTO.getCodDominio()).getId());
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new NotFoundException("Dominio " + findTributiDTO.getCodDominio() + " non censito in Anagrafica");
			}
			filter.setOffset(findTributiDTO.getOffset());
			filter.setLimit(findTributiDTO.getLimit());
			filter.getFilterSortList().addAll(findTributiDTO.getFieldSortList());

			return new FindTributiDTOResponse(ibanAccreditoBD.count(filter), ibanAccreditoBD.findAll(filter));
		} finally {
			bd.closeConnection();
		}
	}
	
	public GetTributoDTOResponse getTributo(GetTributoDTO getTributoDTO) throws NotAuthorizedException, NotFoundException, ServiceException {
		BasicBD bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
		try {
			Set<String> domini = AclEngine.getDominiAutorizzati(getTributoDTO.getUser(), Servizio.Anagrafica_PagoPa);
			
			if(domini != null && !domini.contains(getTributoDTO.getCodDominio())) {
				throw new NotAuthorizedException("L'utente autenticato non e' autorizzato in lettura ai servizi " + Servizio.Anagrafica_PagoPa + " per il dominio " + getTributoDTO.getCodDominio());
			}
			
			Dominio dominio = null;
			try {
				dominio = AnagraficaManager.getDominio(bd, getTributoDTO.getCodDominio());
			} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
				throw new NotFoundException("Dominio " + getTributoDTO.getCodDominio() + " non censito in Anagrafica");
			}
			GetTributoDTOResponse response = new GetTributoDTOResponse(AnagraficaManager.getTributo(bd, dominio.getId(), getTributoDTO.getCodTributo()));
			return response;
		} catch (org.openspcoop2.generic_project.exception.NotFoundException e) {
			throw new NotFoundException("Tributo " + getTributoDTO.getCodTributo() + " non censito in Anagrafica per il dominio " + getTributoDTO.getCodDominio());
		} finally {
			bd.closeConnection();
		}
	}
	
}