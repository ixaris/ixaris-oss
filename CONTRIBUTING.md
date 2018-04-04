# Deploying to maven central

See http://central.sonatype.org/pages/ossrh-guide.html and https://maven.apache.org/guides/mini/guide-central-repository-upload.html

1. Signup at https://issues.sonatype.org/secure/Signup!default.jspa and ask Brian or Aldrin to tell Sonatype to add you to the com.ixaris.oss group
2. Generate a PGP Signature using `gpg --gen-key` (on windows it's best to install [gpg4win](https://www.gpg4win.org/))
3. run `gpg --extract -a` to extract ascii version of your key and copy paste it (including --- header and footer) at http://pgp.mit.edu/
4. update your settings.xml (the password can be replaced by an encrypted password using `mvn -ep`)
```xml
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>your_sonatype_jira_username</username>
            <password>your_sonatype_jira_password</password>
        </server>
    </servers>
    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
            </properties>
        </profile>
    </profiles>
</settings>
```
5. run `mvn deploy`
