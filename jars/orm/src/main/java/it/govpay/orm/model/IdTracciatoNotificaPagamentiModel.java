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
package it.govpay.orm.model;

import it.govpay.orm.IdTracciatoNotificaPagamenti;

import org.openspcoop2.generic_project.beans.AbstractModel;
import org.openspcoop2.generic_project.beans.IField;
import org.openspcoop2.generic_project.beans.Field;
import org.openspcoop2.generic_project.beans.ComplexField;


/**     
 * Model IdTracciatoNotificaPagamenti 
 *
 * @author Giovanni Bussu (bussu@link.it)
 * @author Lorenzo Nardi (nardi@link.it)
 * @author $Author$
 * @version $Rev$, $Date$
 */
public class IdTracciatoNotificaPagamentiModel extends AbstractModel<IdTracciatoNotificaPagamenti> {

	public IdTracciatoNotificaPagamentiModel(){
	
		super();
	
		this.ID_TRACCIATO_NOTIFICA_PAGAMENTI = new Field("idTracciatoNotificaPagamenti",long.class,"id-tracciato-notifica-pagamenti",IdTracciatoNotificaPagamenti.class);
	
	}
	
	public IdTracciatoNotificaPagamentiModel(IField father){
	
		super(father);
	
		this.ID_TRACCIATO_NOTIFICA_PAGAMENTI = new ComplexField(father,"idTracciatoNotificaPagamenti",long.class,"id-tracciato-notifica-pagamenti",IdTracciatoNotificaPagamenti.class);
	
	}
	
	

	public IField ID_TRACCIATO_NOTIFICA_PAGAMENTI = null;
	 

	@Override
	public Class<IdTracciatoNotificaPagamenti> getModeledClass(){
		return IdTracciatoNotificaPagamenti.class;
	}
	
	@Override
	public String toString(){
		if(this.getModeledClass()!=null){
			return this.getModeledClass().getName();
		}else{
			return "N.D.";
		}
	}

}