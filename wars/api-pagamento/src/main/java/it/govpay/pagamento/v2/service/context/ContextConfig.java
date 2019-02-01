package it.govpay.pagamento.v2.service.context;

import it.govpay.core.utils.GovpayConfig;

/**
 * ContextConfig configurazione del context per le Api V2
 * 
 * @author pintori
 *
 */
public class ContextConfig extends org.openspcoop2.utils.service.context.ContextConfig {

	public ContextConfig() {
		super();
		
		try {
			Integer clusterId = GovpayConfig.getInstance().getClusterId();
			if(clusterId != null)
				this.setClusterId(clusterId.toString());
			else 
				this.setClusterId(GovpayConfig.getInstance().getAppName());
			
			this.setDump(GovpayConfig.getInstance().isContextDumpEnabled());
			this.setEmitTransaction(true);
			this.setServiceType("GovPay");
			this.setServiceVersion(2);
		} catch(Exception e) {
			
		}
	}
}
