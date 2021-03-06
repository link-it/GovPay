Feature: Validazione sintattica Operatori

Background:

* callonce read('classpath:utils/common-utils.feature')
* callonce read('classpath:configurazione/v1/anagrafica_estesa.feature')
* def basicAutenticationHeader = getBasicAuthenticationHeader( { username: govpay_backoffice_user, password: govpay_backoffice_password } )
* def backofficeBaseurl = getGovPayApiBaseUrl({api: 'backoffice', versione: 'v1', autenticazione: 'basic'})
* def loremIpsum = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus non neque vestibulum, porta eros quis, fringilla enim. Nam sit amet justo sagittis, pretium urna et, convallis nisl. Proin fringilla consequat ex quis pharetra. Nam laoreet dignissim leo. Ut pulvinar odio et egestas placerat. Quisque tincidunt egestas orci, feugiat lobortis nisi tempor id. Donec aliquet sed massa at congue. Sed dictum, elit id molestie ornare, nibh augue facilisis ex, in molestie metus enim finibus arcu. Donec non elit dictum, dignissim dui sed, facilisis enim. Suspendisse nec cursus nisi. Ut turpis justo, fermentum vitae odio et, hendrerit sodales tortor. Aliquam varius facilisis nulla vitae hendrerit. In cursus et lacus vel consectetur.'
* callonce read('classpath:configurazione/v1/anagrafica_unita.feature')
* def operatore = 
"""
{
  ragioneSociale: 'Mario Rossi',
  domini: ['#(idDominio)'],
  tipiPendenza: ['#(codLibero)'],
  acl: [ { servizio: 'Pagamenti', autorizzazioni: [ 'R', 'W' ] } ],
  abilitato: true
}
"""
          
Scenario Outline: Sintassi errata nel campo (<field>)

* set operatore.<field> = <fieldValue>

Given url backofficeBaseurl
And path 'operatori', 'MarioRossi'
And headers basicAutenticationHeader
And request operatore
When method put
Then status 422

* match response == { categoria: 'RICHIESTA', codice: 'SEMANTICA', descrizione: 'Richiesta non valida', dettaglio: '#notnull' }
* match response.dettaglio contains <fieldResponse>

Examples:
| field | fieldValue | fieldResponse |
| domini | ['#(idDominio)','12345654321'] | 'dominio' |
| tipiPendenza | ['#(codLibero)','12345'] | 'tipo pendenza' |
| domini | [ { idDominio: '#(idDominio)', unitaOperative: [ '00000000000'] } ] | 'unita' |
| domini | [ { idDominio: '#(idDominio_2)', unitaOperative: [ '#(idUnitaOperativa2)' ] } ] | 'unita' |
| domini | [ { idDominio: '#(idDominio)', unitaOperative: [ '#(idUnitaOperativa)', '00000000000' ] } ] | 'unita' |
| domini | [ '00000000000'] | 'dominio' |
| domini | [ { idDominio: '00000000000' } ] | 'dominio' |