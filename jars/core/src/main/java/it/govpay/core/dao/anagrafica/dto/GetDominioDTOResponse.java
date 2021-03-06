package it.govpay.core.dao.anagrafica.dto;

import java.util.List;

import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.IbanAccredito;
import it.govpay.bd.model.TipoVersamentoDominio;
import it.govpay.bd.model.Tributo;
import it.govpay.bd.model.UnitaOperativa;

public class GetDominioDTOResponse {
	
	private Dominio dominio;
	private List<UnitaOperativa> uo;
	private List<IbanAccredito> iban;
	private List<Tributo> tributi;
	private List<TipoVersamentoDominio> tipiVersamentoDominio;
	
	public GetDominioDTOResponse(Dominio dominio) {
		this.dominio = dominio;
	}

	public Dominio getDominio() {
		return this.dominio;
	}

	public void setDominio(Dominio dominio) {
		this.dominio = dominio;
	}

	public List<IbanAccredito> getIban() {
		return this.iban;
	}

	public void setIban(List<IbanAccredito> iban) {
		this.iban = iban;
	}

	public List<Tributo> getTributi() {
		return this.tributi;
	}

	public void setTributi(List<Tributo> tributi) {
		this.tributi = tributi;
	}

	public List<UnitaOperativa> getUo() {
		return this.uo;
	}

	public void setUo(List<UnitaOperativa> uo) {
		this.uo = uo;
	}

	public List<TipoVersamentoDominio> getTipiVersamentoDominio() {
		return tipiVersamentoDominio;
	}

	public void setTipiVersamentoDominio(List<TipoVersamentoDominio> tipiVersamentoDominio) {
		this.tipiVersamentoDominio = tipiVersamentoDominio;
	}

}
