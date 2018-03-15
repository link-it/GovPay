/**
 * 
 */
package it.govpay.rs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.MDC;

import it.govpay.core.exceptions.BaseExceptionV1;
import it.govpay.core.exceptions.NotAuthorizedException;
import it.govpay.core.rs.v1.beans.JSONSerializable;
import it.govpay.core.rs.v1.beans.base.FaultBean;
import it.govpay.core.rs.v1.beans.base.FaultBean.CategoriaEnum;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.log.MessageLoggingHandlerUtils;

/**
 * @author Bussu Giovanni (bussu@link.it)
 * @author  $Author: bussu $
 * @version $ Rev: 12563 $, $Date: 22 feb 2018 $
 * 
 */
public abstract class BaseController {

	protected Logger log;
	protected String nomeServizio;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	
	public BaseController(String nomeServizio, Logger log) {
		this.log = log;
		this.nomeServizio = nomeServizio;
	}
	
	public void setRequestResponse(HttpServletRequest request,HttpServletResponse response) {
		this.response = response;
		this.request = request;
	}
	
	public HttpServletRequest getRequest() {
		return this.request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return this.response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public int getVersione() {
		return 1;
	}
	
	public void setupContext(UriInfo uriInfo, HttpHeaders rsHttpHeaders,String nomeOperazione) throws ServiceException {
		GpContext ctx = new GpContext(uriInfo,rsHttpHeaders, this.request, nomeOperazione, this.nomeServizio, GpContext.TIPO_SERVIZIO_GOVPAY_JSON, this.getVersione());
		MDC.put("op", ctx.getTransactionId());
		GpThreadLocal.set(ctx);
	}
	
	public void logResponse(UriInfo uriInfo, HttpHeaders rsHttpHeaders, String nomeOperazione, Object o) throws IOException, NotAuthorizedException {
		this.logResponse(uriInfo, rsHttpHeaders, nomeOperazione, o, null);
	}
	

	public void logResponse(UriInfo uriInfo, HttpHeaders rsHttpHeaders, String nomeOperazione, Object o, Integer responseCode) throws IOException, NotAuthorizedException {
		if(o != null && o instanceof JSONSerializable) {
			this.logResponse(uriInfo, rsHttpHeaders, nomeOperazione, ((JSONSerializable) o).toJSON(null).getBytes(), responseCode);
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(o);
			this.logResponse(uriInfo, rsHttpHeaders, nomeOperazione, json.getBytes(), responseCode);
		}
	}
	
	public void logRequest(UriInfo uriInfo, HttpHeaders rsHttpHeaders,String nomeOperazione, ByteArrayOutputStream baos) throws NotAuthorizedException {
		MessageLoggingHandlerUtils.logToSystemOut(uriInfo, rsHttpHeaders, this.request,baos,
				nomeOperazione, this.nomeServizio, GpContext.TIPO_SERVIZIO_GOVPAY_JSON, this.getVersione(), this.log, false);
	}
	
	public void logRequest(UriInfo uriInfo, HttpHeaders rsHttpHeaders,String nomeOperazione, byte[] baos) throws NotAuthorizedException{
		MessageLoggingHandlerUtils.logToSystemOut(uriInfo, rsHttpHeaders, this.request,baos,
				nomeOperazione, this.nomeServizio, GpContext.TIPO_SERVIZIO_GOVPAY_JSON, this.getVersione(), this.log, false);
	}

	public void logResponse(UriInfo uriInfo, HttpHeaders rsHttpHeaders,String nomeOperazione, ByteArrayOutputStream baos) throws NotAuthorizedException {
		MessageLoggingHandlerUtils.logToSystemOut(uriInfo, rsHttpHeaders, this.request,baos,
				nomeOperazione, this.nomeServizio, GpContext.TIPO_SERVIZIO_GOVPAY_JSON, this.getVersione(), this.log, true);
	}
	
	public void logResponse(UriInfo uriInfo, HttpHeaders rsHttpHeaders,String nomeOperazione,byte[] bytes) throws NotAuthorizedException {
		MessageLoggingHandlerUtils.logToSystemOut(uriInfo, rsHttpHeaders, this.request,bytes,
				nomeOperazione, this.nomeServizio, GpContext.TIPO_SERVIZIO_GOVPAY_JSON, this.getVersione(), this.log, true);
	}
	
	public void logResponse(UriInfo uriInfo, HttpHeaders rsHttpHeaders,String nomeOperazione,byte[] bytes, Integer responseCode) throws NotAuthorizedException {
		MessageLoggingHandlerUtils.logToSystemOut(uriInfo, rsHttpHeaders, this.request,bytes,
				nomeOperazione, this.nomeServizio, GpContext.TIPO_SERVIZIO_GOVPAY_JSON, this.getVersione(), this.log, true, responseCode);
	}
	
	public URI getServicePath(UriInfo uriInfo) throws URISyntaxException {
		String baseUri = uriInfo.getBaseUri().toString();
		String requestUri = uriInfo.getRequestUri().toString();
		int idxOfBaseUri = requestUri.indexOf(baseUri);
		
		String servicePathwithParameters = requestUri.substring((idxOfBaseUri + baseUri.length()) - 1);
		
		return new URI(servicePathwithParameters);
	}
	
	protected Response handleException(UriInfo uriInfo, HttpHeaders httpHeaders, String methodName, Exception e) {
		log.error("Errore interno durante "+methodName+": " + e.getMessage(), e);
		FaultBean respKo = new FaultBean();
		respKo.setCategoria(CategoriaEnum.INTERNO);
		respKo.setCodice("");
		respKo.setDescrizione(e.getMessage());
		try {
			this.logResponse(uriInfo, httpHeaders, methodName, respKo.toJSON(null), 500);
		}catch(Exception e1) {
			log.error("Errore durante il log della risposta", e1);
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(respKo.toJSON(null)).build();
	}

	protected Response handleBaseException(UriInfo uriInfo, HttpHeaders httpHeaders, String methodName, BaseExceptionV1 e) {
		log.error("Errore ("+e.getClass().getSimpleName()+") durante "+methodName+": "+ e.getMessage(), e);
		FaultBean respKo = new FaultBean();
		respKo.setCategoria(e.getCategoria());
		respKo.setCodice(e.getCode());
		respKo.setDescrizione(e.getMessage());
		respKo.setDettaglio(e.getDetails());
		try {
			this.logResponse(uriInfo, httpHeaders, methodName, respKo.toJSON(null), e.getTransportErrorCode());
		}catch(Exception e1) {
			log.error("Errore durante il log della risposta  "+methodName+":", e1.getMessage(), e);
		}
		return Response.status(e.getTransportErrorCode()).entity(respKo.toJSON(null)).build();
	}


}
