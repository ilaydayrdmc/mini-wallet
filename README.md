# Mini Wallet

[![CI](https://github.com/ilaydayrdmc/mini-wallet/actions/workflows/ci.yml/badge.svg)](https://github.com/ilaydayrdmc/mini-wallet/actions/workflows/ci.yml)

Küçük bir dijital cüzdan uygulaması: kayıt ol ve giriş yap, hesap aç, para yatır, para çek, hesaplar arası transfer yap ve işlem geçmişini gör.

Spring Boot ile yazılmış katmanlı bir REST API (JWT ile korumalı), React arayüzü ve PostgreSQL'den oluşur. Tamamı Docker ile tek komutla ayağa kalkar.

![Mini Wallet ekran görüntüsü](docs/screenshot.png)

## Özellikler

- Kullanıcı kaydı ve girişi (BCrypt ile saklanan şifre, JWT ile oturum)
- Her kullanıcı yalnızca kendi hesaplarını görür ve kullanır
- Hesap açma ve listeleme
- Para yatırma ve çekme (yetersiz bakiye kontrolüyle)
- Hesaplar arası transfer, başka bir kullanıcının hesabına hesap numarasıyla da (tek veritabanı transaction'ı içinde)
- Sayfalı, en yeniden eskiye işlem geçmişi (her kayıtta işlem sonrası bakiye)
- Doğrulama ve standart (RFC 7807 `ProblemDetail`) hata yanıtları

## Teknolojiler

| Katman | Teknoloji |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Web, Spring Data JPA (Hibernate), Spring Security (OAuth2 Resource Server, JWT), Bean Validation |
| Veritabanı | PostgreSQL 16, Flyway (migration) |
| Frontend | React 19, Vite |
| Test | JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers, Playwright (uçtan uca) |
| Altyapı | Docker, Docker Compose (Nginx ile frontend), Maven Wrapper |
| CI | GitHub Actions (backend testleri, frontend lint/build, gerçek tarayıcıda uçtan uca testler) |

## Hızlı başlangıç

Gereken tek şey [Docker](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/ilaydayrdmc/mini-wallet.git
cd mini-wallet
docker compose up -d --build
```

| Servis | Adres |
|---|---|
| Arayüz | http://localhost:3000 |
| API | http://localhost:8080/api |
| PostgreSQL | `localhost:5432` (kullanıcı/şifre/veritabanı: `wallet`) |

Arayüzü açıp **Kayıt ol** ile bir kullanıcı oluştur, ardından hesap aç. Durdurmak için `docker compose down`, verileri de silmek için `docker compose down -v`.

### JWT sırrı

Token'lar `JWT_SECRET` ile imzalanır. Repodaki varsayılan değer **yalnızca yerel geliştirme içindir** ve herkese açıktır. Gerçek bir ortamda en az 32 karakterlik rastgele bir değerle değiştirilmelidir:

```bash
JWT_SECRET="en-az-32-karakterlik-rastgele-bir-deger" docker compose up -d --build
```

Sır 32 karakterden kısaysa uygulama açılmaz.

## Geliştirme ortamı

Docker yerine kodu yerelde çalıştırmak için JDK 17 ve Node.js 20.19+ (veya 22.12+) gerekir. Maven kurulumu gerekmez, Maven Wrapper kullanılır.

```bash
# 1. Sadece veritabanını başlat
docker compose up -d db

# 2. Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend (http://localhost:5173), başka bir terminalde
cd frontend
npm install
npm run dev
```

Vite geliştirme sunucusu `/api` isteklerini `localhost:8080`'e iletir, bu yüzden CORS ayarı gerekmez. Docker kurulumunda aynı işi Nginx yapar.

## Testler

```bash
cd backend
./mvnw test          # Windows: .\mvnw.cmd test
```

Testler çalışırken **Docker çalışıyor olmalıdır**: entegrasyon testleri [Testcontainers](https://testcontainers.com/) ile geçici bir PostgreSQL container'ı başlatır, ayrıca veritabanı kurmak gerekmez.

| Test sınıfı | Ne doğrular |
|---|---|
| `AccountServiceTest`, `TransferServiceTest` | Mockito ile servis birim testleri: yatırma, çekme, transfer, yetersiz bakiye, olmayan hesap, sahiplik, kilit sırası |
| `AuthServiceTest`, `TokenServiceTest` | Şifre hash'lenir, kullanıcı adı normalize edilir, yanlış şifre ve bilinmeyen kullanıcı aynı hatayı verir. Token içeriği doğru; değiştirilmiş, başka sırla imzalanmış ve süresi dolmuş token reddedilir |
| `AccountControllerTest`, `TransferControllerTest`, `AuthControllerTest` | `@WebMvcTest` ile yalnızca web katmanı: HTTP durum kodları, giriş doğrulama, hata gövdeleri, sayfalama parametreleri |
| `SecurityRulesTest` | Hangi yollar herkese açık, hangileri token istiyor; sahte token ve `Basic` başlığı reddedilir |
| `AuthFlowIntegrationTest` | Gerçek PostgreSQL ve gerçek JWT ile kayıt → giriş → korumalı endpoint; imzası bozulmuş token, yanlış şifre, çift kayıt |
| `OwnershipIntegrationTest` | İki gerçek kullanıcıyla: herkes yalnızca kendi hesaplarını görür, başkasının hesabında yatırma/çekme/geçmiş/transfer `404` verir |
| `ConcurrencyIntegrationTest` | Gerçek PostgreSQL üzerinde eşzamanlılık: yatırmada güncelleme kaybolmaz, eşzamanlı çekmeler bakiyeyi eksiye düşüremez, karşılıklı transferler deadlock yapmaz ve toplam para korunur |
| `WalletApplicationTests` | Flyway migration'ları boş bir veritabanına uygulanır, Hibernate şema doğrulaması geçer |

Güvenlik ve kilit testlerinin gerçekten koruma sağladığı, ilgili kod geçici olarak bozularak denenmiştir: satır kilidi (`FOR UPDATE`), sahiplik koşulu ve `anyRequest().authenticated()` kuralı kaldırıldığında ilgili testler kırılır.

### Arayüz testleri (uçtan uca)

[Playwright](https://playwright.dev/) testleri, ayakta olan gerçek uygulamaya (nginx → Spring Boot → PostgreSQL) karşı gerçek bir tarayıcıda çalışır. Önce uygulamayı başlat:

```bash
docker compose up -d --build

cd frontend
npm install
npx playwright install chromium      # yerelde kurulu Chrome varsa gerekmez
npm run test:e2e
```

Kapsam (`frontend/e2e/wallet.spec.js`): kayıt, giriş, çıkış, oturumun sayfa yenilemede sürmesi, yanlış şifre, çift kayıt, geçersiz/süresi dolmuş token, hesap açma, yatırma, çekme, yetersiz bakiye, başka bir kullanıcının hesabına numarayla transfer, olmayan hesaba transfer, kullanıcıların birbirinin hesabını görmemesi ve dar (375px) ekranda yatay taşma olmaması.

Yerelde sistemdeki Chrome kullanılır, CI'da Playwright'in Chromium'u indirilir. Arayüz kodunda geçici bozmalar yapılarak (giriş isteğinden token'ı ayırma kuralının kaldırılması, `401` yakalamanın kapatılması) testlerin gerçekten kırıldığı doğrulanmıştır.

Lint ve derleme kontrolü: `cd frontend && npm run lint && npm run build`.

## API

Temel adres: `/api`. İstek ve yanıtlar JSON'dur. **Korumalı** uç noktalar `Authorization: Bearer <token>` başlığı ister.

| Metot | Yol | Korumalı | Açıklama |
|---|---|---|---|
| `GET` | `/hello` | hayır | Basit sağlık denemesi |
| `POST` | `/auth/register` | hayır | Kayıt. Gövde: `{"username": "ayse", "password": "en-az-8-karakter"}` |
| `POST` | `/auth/login` | hayır | Giriş. Yanıt: `{"accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600}` |
| `POST` | `/accounts` | evet | Hesap açar. Gövde: `{"ownerName": "Ayşe"}` |
| `GET` | `/accounts` | evet | Kendi hesaplarını listeler |
| `POST` | `/accounts/{id}/deposit` | evet | Para yatırır. Gövde: `{"amount": 100.50}` |
| `POST` | `/accounts/{id}/withdraw` | evet | Para çeker. Gövde: `{"amount": 40}` |
| `POST` | `/transfers` | evet | Transfer. Gövde: `{"fromAccountId": 1, "toAccountId": 2, "amount": 25}`. Gönderen hesap size ait olmalı, alıcı herhangi bir hesap olabilir |
| `GET` | `/accounts/{id}/transactions?page=0&size=20` | evet | Sayfalı işlem geçmişi (`size` en fazla 100) |

Hata durumları:

| Durum | HTTP kodu |
|---|---|
| Geçersiz istek (boş isim, tutar < 0.01, kısa şifre, hatalı sayfa parametresi) | `400` |
| Kendi hesabına transfer | `400` |
| Token yok, geçersiz veya süresi dolmuş | `401` |
| Yanlış kullanıcı adı veya şifre | `401` |
| Hesap bulunamadı **veya başkasına ait** | `404` |
| Kullanıcı adı zaten alınmış | `409` |
| Yetersiz bakiye | `422` |

Örnek:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" -d '{"username":"ayse","password":"gizli-sifre-123"}'

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"ayse","password":"gizli-sifre-123"}' \
  | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"ownerName":"Ayse Yilmaz"}'

curl -X POST http://localhost:8080/api/accounts/1/deposit \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"amount":100.50}'
```

## Mimari

```
mini-wallet/
├── backend/                       Spring Boot uygulaması
│   └── src/main/java/com/miniwallet/wallet/
│       ├── controller/            HTTP katmanı (istek/yanıt)
│       ├── service/               İş mantığı ve transaction sınırları
│       ├── repository/            Spring Data JPA arayüzleri
│       ├── entity/                Veritabanı tabloları (User, Account, Transaction)
│       ├── dto/                   API giriş/çıkış modelleri
│       ├── security/              Token'dan geçerli kullanıcıyı okuma
│       ├── config/                Güvenlik, JWT ve şifre yapılandırması
│       └── exception/             Özel hatalar ve global hata yakalayıcı
├── frontend/                      React + Vite arayüzü
│   └── src/
│       ├── api.js                 Backend ile konuşan tek yer (token'ı ekler, 401'i yakalar)
│       ├── auth.js                Token'ın saklanması
│       └── components/            Giriş, hesap listesi, panel, transfer, geçmiş
├── docs/                          Ekran görüntüsü
├── docker-compose.yml             db + app + frontend
└── .github/workflows/ci.yml       CI
```

İstek akışı: `Controller → Service → Repository → PostgreSQL`. Entity'ler API'ye doğrudan açılmaz, her uç nokta kendi DTO'sunu kullanır.

### Tasarım kararları

**Para ve eşzamanlılık**

- **Para tutarları `BigDecimal`** ile (`NUMERIC(19,2)`) tutulur ve karşılaştırılır, `double` kullanılmaz.
- **Tek transaction:** Yatırma, çekme ve transfer `@Transactional` içindedir. Bakiye güncellemesi ve işlem kaydı birlikte kaydedilir ya da hiç kaydedilmez.
- **Pessimistic locking:** Bakiye değiştiren işlemler hesabı `SELECT ... FOR UPDATE` ile kilitler. Aynı hesaba eşzamanlı işlemler sırayla yapılır, güncellemeler birbirini ezmez.
- **Deadlock önleme:** Transferde iki hesap her zaman küçük id'den büyüğe doğru kilitlenir. Karşılıklı transferler (A→B ve B→A) birbirini beklemez.
- **İşlem geçmişi** her kayıtta `balanceAfter` saklar, böylece geçmiş görüntülenirken bakiye yeniden hesaplanmaz.

**Kimlik doğrulama ve yetkilendirme**

- **Şifreler BCrypt ile hash'lenir**, düz saklanmaz ve API yanıtlarında hiç yer almaz. Kullanıcı adları küçük harfe çevrilir, aynı kullanıcı adının iki kez alınması veritabanındaki `UNIQUE` kısıtla da engellenir (eşzamanlı kayıtlar dahil).
- **Oturum yok (stateless):** Giriş yapınca HS256 ile imzalanmış, 1 saat geçerli bir JWT döner. Her istek `Authorization: Bearer` başlığıyla kimliğini taşır. Bu modelde tarayıcı token'ı otomatik göndermediği için CSRF koruması gereksizdir ve kapalıdır.
- **Kullanıcı bilgisi sızdırılmaz:** Yanlış şifre ve bilinmeyen kullanıcı aynı hatayı verir; kullanıcı yoksa da bir BCrypt karşılaştırması yapılır, böylece yanıt süresinden hangi kullanıcıların kayıtlı olduğu anlaşılmaz.
- **Sahiplik (IDOR koruması):** Kullanıcı kimliği her zaman doğrulanmış token'dan alınır, istek gövdesinden asla. Sahiplik sorgunun içindedir (`where id = ? and user_id = ?`), başkasının hesabı "bulunamadı" (`404`) gibi davranır, böylece hesapların var olup olmadığı da sızmaz.

**Veritabanı**

- **Şema Flyway ile yönetilir** (`backend/src/main/resources/db/migration`). Hibernate yalnızca `validate` modunda çalışır, tabloları kendisi oluşturmaz veya değiştirmez. Uygulanmış bir migration dosyası değiştirilmez, değişiklik yeni bir sürüm dosyasıyla yapılır.

## Bilinen sınırlar

- **Token tarayıcıda `localStorage`'da saklanır.** Sayfadaki herhangi bir JavaScript (XSS durumunda) okuyabilir. Daha sıkı yöntem `HttpOnly` cookie'dir.
- **Yenileme (refresh) token'ı ve token iptali yok.** Token 1 saat geçerlidir, süresi dolunca yeniden giriş gerekir. Çıkış yapmak token'ı yalnızca tarayıcıdan siler.
- **Giriş denemelerine hız sınırı yok** (kaba kuvvet denemelerine karşı savunmasızdır). Şifre sıfırlama ve e-posta doğrulaması da yok.
- Varsayılan JWT sırrı ve veritabanı şifreleri (`wallet/wallet`) yerel geliştirme içindir ve repoda açıkça yazılıdır.
- Transferde alıcı hesap, sahibi kim olursa olsun kilitlenir. Kötü niyetli bir kullanıcı başkasının hesabını kısa süre meşgul edebilir.
- `ownerName` alanı hesabın serbest metin adıdır, kullanıcı adına bağlı değildir.
- Para birimi yalnızca arayüzde ₺ olarak gösterilir, backend para birimi tutmaz.
- Arayüz bileşenleri için birim testi yoktur; arayüz yalnızca uçtan uca (Playwright) testlerle doğrulanır.
