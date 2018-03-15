package it.govpay.rs.v1.controllers.base;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;

import it.govpay.core.dao.pagamenti.IncassiDAO;
import it.govpay.core.dao.pagamenti.dto.LeggiIncassoDTO;
import it.govpay.core.dao.pagamenti.dto.LeggiIncassoDTOResponse;
import it.govpay.core.dao.pagamenti.dto.ListaIncassiDTO;
import it.govpay.core.dao.pagamenti.dto.ListaIncassiDTOResponse;
import it.govpay.core.dao.pagamenti.dto.RichiestaIncassoDTO;
import it.govpay.core.dao.pagamenti.dto.RichiestaIncassoDTOResponse;
import it.govpay.core.exceptions.BaseExceptionV1;
import it.govpay.core.rs.v1.beans.Incasso;
import it.govpay.core.rs.v1.beans.IncassoPost;
import it.govpay.core.rs.v1.beans.ListaIncassi;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.model.IAutorizzato;
import it.govpay.rs.BaseRsService;
import it.govpay.rs.v1.beans.converter.IncassiConverter;
import net.sf.json.JsonConfig;



public class IncassiController extends it.govpay.rs.BaseController {
	
	public IncassiController(String nomeServizio,Logger log) {
		super(nomeServizio,log);
	}


    public Response incassiGET(IAutorizzato user, UriInfo uriInfo, HttpHeaders httpHeaders , Integer pagina, Integer risultatiPerPagina) {
    	String methodName = "incassiGET";  
		GpContext ctx = null;
		ByteArrayOutputStream baos= null;
		this.log.info("Esecuzione " + methodName + " in corso..."); 
		String campi = null;
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			
			// Parametri - > DTO Input
			
			ListaIncassiDTO listaIncassoDTO = new ListaIncassiDTO(user);
			
			listaIncassoDTO.setPagina(pagina);
			listaIncassoDTO.setLimit(risultatiPerPagina);
			
			// INIT DAO
			
			IncassiDAO incassiDAO = new IncassiDAO();
			
			// CHIAMATA AL DAO
			
			ListaIncassiDTOResponse listaIncassiDTOResponse = incassiDAO.listaIncassi(listaIncassoDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			List<it.govpay.core.rs.v1.beans.Incasso> listaIncassi = new ArrayList<it.govpay.core.rs.v1.beans.Incasso>();
			for(it.govpay.bd.model.Incasso i : listaIncassiDTOResponse.getResults()) {
				listaIncassi.add(IncassiConverter.toRsModel(i));
			}
			
			ListaIncassi response = new ListaIncassi(listaIncassi, this.getServicePath(uriInfo),
					listaIncassiDTOResponse.getTotalResults(), pagina, risultatiPerPagina);
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(campi), 200);
			this.log.info("Esecuzione " + methodName + " completata."); 
			return Response.status(Status.OK).entity(response.toJSON(campi)).build();
			
		}catch (BaseExceptionV1 e) {
			return handleException(uriInfo, httpHeaders, methodName, e);
		}catch (Exception e) {
			return handleException(uriInfo, httpHeaders, methodName, e);
		} finally {
			if(ctx != null) ctx.log();
		}
    }


    public Response incassiIdDominioIdIncassoGET(IAutorizzato user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, String idIncasso) {
    	String methodName = "incassiIdDominioIdIncassoGET";  
		GpContext ctx = null;
		ByteArrayOutputStream baos= null;
		this.log.info("Esecuzione " + methodName + " in corso..."); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			
			// Parametri - > DTO Input
			
			LeggiIncassoDTO leggiIncassoDTO = new LeggiIncassoDTO(user);
			leggiIncassoDTO.setIdDominio(idDominio);
			leggiIncassoDTO.setIdIncasso(idIncasso);

			// INIT DAO
			
			IncassiDAO incassiDAO = new IncassiDAO();
			
			// CHIAMATA AL DAO
			
			LeggiIncassoDTOResponse leggiIncassoDTOResponse = incassiDAO.leggiIncasso(leggiIncassoDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			Incasso response = IncassiConverter.toRsModel(leggiIncassoDTOResponse.getIncasso());
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(null), 200);
			this.log.info("Esecuzione " + methodName + " completata."); 
			return Response.status(Status.OK).entity(response.toJSON(null)).build();
			
		}catch (BaseExceptionV1 e) {
			return handleException(uriInfo, httpHeaders, methodName, e);
		}catch (Exception e) {
			return handleException(uriInfo, httpHeaders, methodName, e);
		} finally {
			if(ctx != null) ctx.log();
		}
    }

    public Response incassiIdDominioPOST(IAutorizzato user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, java.io.InputStream is) {
    	String methodName = "incassiIdDominioPOST"; 
		GpContext ctx = null;
		ByteArrayOutputStream baos= null;
		this.log.info("Esecuzione " + methodName + " in corso..."); 
		try{
			baos = new ByteArrayOutputStream();
			// salvo il json ricevuto
			BaseRsService.copy(is, baos);
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			
			it.govpay.core.rs.v1.beans.IncassoPost incasso = (IncassoPost) it.govpay.core.rs.v1.beans.IncassoPost.parse(baos.toString(), it.govpay.core.rs.v1.beans.IncassoPost.class, new JsonConfig());
			RichiestaIncassoDTO richiestaIncassoDTO = IncassiConverter.toRichiestaIncassoDTO(incasso, idDominio, user);
			
			IncassiDAO incassiDAO = new IncassiDAO();
			
			RichiestaIncassoDTOResponse richiestaIncassoDTOResponse = incassiDAO.richiestaIncasso(richiestaIncassoDTO);
			
			Incasso incassoExt = IncassiConverter.toRsModel(richiestaIncassoDTOResponse.getIncasso());
			
			Status responseStatus = richiestaIncassoDTOResponse.isCreated() ?  Status.CREATED : Status.OK;
			
			this.logResponse(uriInfo, httpHeaders, methodName, incassoExt.toJSON(null), responseStatus.getStatusCode());
			this.log.info("Esecuzione " + methodName + " completata."); 
			return Response.status(responseStatus).entity(incassoExt.toJSON(null)).build();
		}catch (BaseExceptionV1 e) {
			return handleException(uriInfo, httpHeaders, methodName, e);
		}catch (Exception e) {
			return handleException(uriInfo, httpHeaders, methodName, e);
		} finally {
			if(ctx != null) ctx.log();
		}
    }
}


