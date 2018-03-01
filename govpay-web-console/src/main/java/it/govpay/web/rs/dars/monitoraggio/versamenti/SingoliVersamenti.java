package it.govpay.web.rs.dars.monitoraggio.versamenti;

import javax.ws.rs.Path;

import org.openspcoop2.utils.LoggerWrapperFactory;
import org.slf4j.Logger;

import it.govpay.model.Acl.Servizio;
import it.govpay.web.rs.dars.base.DarsService;
import it.govpay.web.rs.dars.handler.IDarsHandler;

@Path("/dars/singoliVersamenti")
public class SingoliVersamenti extends DarsService {

	public SingoliVersamenti() {
		super();
	}
	
	Logger log = LoggerWrapperFactory.getLogger(SingoliVersamenti.class);
	
	@Override
	public String getNomeServizio() {
		return "singoliVersamenti";
	}

	@Override
	public IDarsHandler<?> getDarsHandler() {
		return new SingoliVersamentiHandler(this.log, this);
	}
	
	@Override
	public String getPathServizio() {
		return "/dars/" + this.getNomeServizio();
	}
	
	@Override
	public Servizio getFunzionalita() {
		return Servizio.PAGAMENTI_E_PENDENZE;
	}
}