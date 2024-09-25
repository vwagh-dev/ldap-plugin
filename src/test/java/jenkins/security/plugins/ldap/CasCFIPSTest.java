package jenkins.security.plugins.ldap;

import hudson.security.LDAPSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.FlagRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// Based on https://github.com/jenkinsci/configuration-as-code-plugin/blob/7766b7ef6153e3e210f257d323244c1f1470a10f/integrations/src/test/java/io/jenkins/plugins/casc/LDAPTest.java
public class CasCFIPSTest {

    @ClassRule
    public static FlagRule<String> fipsSystemPropertyRule =
            FlagRule.systemProperty("jenkins.security.FIPS140.COMPLIANCE", "true");

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
            .set("LDAP_PASSWORD", "SECRET"))
            .around(new JenkinsConfiguredWithCodeRule());
    
    @Test
    @ConfiguredWithCode("casc_ldap_secure.yml")
    public void configure_ldap() {
        final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) Jenkins.get().getSecurityRealm();
        assertEquals(1, securityRealm.getConfigurations().size());
        assertTrue(securityRealm.getUserIdStrategy() instanceof IdStrategy.CaseInsensitive);
        assertTrue(securityRealm.getGroupIdStrategy() instanceof IdStrategy.CaseSensitive);
        final LDAPConfiguration configuration = securityRealm.getConfigurations().get(0);
        assertEquals("ldaps://ldap.acme.com", configuration.getServer());
        assertEquals("SECRET", configuration.getManagerPassword());
        assertEquals("manager", configuration.getManagerDN());
        assertEquals("(&(objectCategory=User)(sAMAccountName={0}))", configuration.getUserSearch());
        assertEquals("(&(cn={0})(objectclass=group))", configuration.getGroupSearchFilter());
        final FromGroupSearchLDAPGroupMembershipStrategy strategy = ((FromGroupSearchLDAPGroupMembershipStrategy) configuration.getGroupMembershipStrategy());
        assertEquals("(&(objectClass=group)(|(cn=GROUP_1)(cn=GROUP_2)))", strategy.getFilter());
    }

    /**
     * Expect an exception when LDAP url is not secure & FIPS is enabled
     */
    //@Test(expected = Exception.class)
    @ConfiguredWithCode("casc.yml")
    public void configure_ldap_for_invalid() {
        try {
            final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) Jenkins.get().getSecurityRealm();
            assertEquals(1, securityRealm.getConfigurations().size());
            final LDAPConfiguration configuration = securityRealm.getConfigurations().get(0);
            assertEquals("ldaps://ldap.acme.com", configuration.getServer());

        }catch (IllegalArgumentException e){
            assertEquals("Invalid configuration", e.getMessage());
            throw e;
        }

    }
}
