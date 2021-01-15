## Signing / Encrypting

### Test Keystore

This command creates a keystore with sender private keys in it:

```
keytool -genkey -keystore emails.p12 -alias sender@mail.com -keyalg RSA -sigalg SHA256withRSA -validity 36500 -keysize 2048 -deststoretype pkcs12
```

This extracts the public key:

```
openssl pkcs12 -in emails.p12 -nokeys -out cert.pem
```

This imports it with recipient alias:

```
keytool -importcert -file cert.pem -alias recipient@mail.com -keystore emails.p12
```