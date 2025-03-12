### HAG - Helse Arbeidsgiver
_________
# Token server + HTTP kataloger

En maskinporten token server og katalog av HTTP requests som automatisk henter tokens.   
Bruker din lokal `kubectl` instans, husk derfor å være pålogget med  `gcloud auth login`.  
Designet for testing i dev miljø og bruker automatisk dev-gcp context.


![](readme/token-server-diagram.png)

## Komme i gang

### Start severen

`./release/bin/start` eller `gradle run`

### Hent en token
http://localhost:4242/token/sykepenger-im-lps-api  

### Sikker commits
Kjør `gradle safe-commit` for å sette opp **git** til å aldri inkludere JWT tokens i prosjektet.  
*(Anbefales om du skal gjøre endringer i Bruno kataloger)*

## Autentisert CURL
Les en dialog fra Dialogporten med id `0194bc95-97b4-7240-961f-9663743d4518` med token for `sykepenger-im-lps-api`
```
curl -X 'GET' \
'https://platform.tt02.altinn.no/dialogporten/api/v1/serviceowner/dialogs/0194bc95-97b4-7240-961f-9663743d4518' \
-H 'accept: application/json' \
-H "Authorization: Bearer $(curl -sX GET http://localhost:4242/token/sykepenger-im-lps-api)" \
| jq
```

## Autentisert HTTP Kataloger

Disse HTTP katalogene henter automatisk nye tokens for requests ved bruk av pre-request scripts [(bruno eksempel)](./readme/bruno-prescript-eksempel.png).



### **Bruno:**
```
brew install bruno && open /Applications/Bruno.app
```
Mac menu bar: `Bruno` > `Open Collection` > `hag-api-testing-katalog/kataloger/dialogporten`

### **Postman:**
```
brew install postman && open /Applications/Postman.app
```
Mac menu bar: `File` > `Import Collection` > `hag-api-testing-katalog/kataloger/postman.json`

## Autentisert Swagger

Øverst på siden settes lenke til swagger.json url og hvilken intern tjeneste tokens hentes fra.


Tokens blir automatisk lagt til i header `Authorization: Bearer [token]` på alle HTTP kall på siden.


Lenke: **http://localhost:4242/swagger**

[![](readme/swagger-eksempel.png)](http://localhost:4242/swagger)

## Endepunkter

Koden for endepunktene ligger i [./src/main/kotlin/Routing.kt](./src/main/kotlin/Routing.kt)

## Hvordan fungerer serveren?

Serveren bruker lokalt konfigurert kubectl config .

**Route: `http://localhost:4242/token/{tjeneste-navn}`**

Serveren finner første substring match for `{tjeneste-navn}` i listen av maskinporten secrets tilgjengelige for helsearbeidsgiver.

Maskinporten JWK secrets for dev-gcp blir "cached" i minne til serveren.

Disse JWK secrets brukes for å hente nye maskinporten tokens for hver request for `{tjeneste-navn}`.


## Obs!

- Om du prøver en å hente token for en ny tjeneste som ikke er "cached" fra tidligere så kan du være utlogget på GCloud.
- JWK secrets kan i sjeldne tilfeller bli utdaterte! Da må serveren startes på nytt. eyJ. eyJ.


## Ekstra

Et [påskeegg](https://www.nrk.no/filmpolitiet/et-annerledes-paskeegg-1.17237361) er at port 4242 som serveren bruker er *hag* delvis skrevet på [T9 tastatur](https://no.wikipedia.org/wiki/T9):

```
 _______________________
| 1     | 2 abc | 3 def |
| -     |       |       |
|_______|_______|_______|
| 4 ghi | 5 jkl | 6 mno |
|       |       |       |
|_______|_______|_______|
| 7 pqrs| 8 tuv | 9 wxyz|
|       |       |       |
|_______|_______|_______|
|   *   |   0   |   #   |
|       |       |       |
|_______|_______|_______|
```