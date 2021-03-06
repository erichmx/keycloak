package org.keycloak.authorization.policy.provider.drools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.kie.api.KieServices;
import org.kie.api.KieServices.Factory;
import org.kie.api.runtime.KieContainer;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DroolsPolicyProviderFactory implements PolicyProviderFactory {

    private KieServices ks;
    private final Map<String, DroolsPolicy> containers = Collections.synchronizedMap(new HashMap<>());
    private DroolsPolicyProvider provider = new DroolsPolicyProvider(policy -> {
        if (!containers.containsKey(policy.getId())) {
            synchronized (containers) {
                update(policy);
            }
        }
        return containers.get(policy.getId());
    });

    @Override
    public String getName() {
        return "Rule";
    }

    @Override
    public String getGroup() {
        return "Rule Based";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return new DroolsPolicyAdminResource(resourceServer, this);
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
        this.ks = Factory.get();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        this.containers.values().forEach(DroolsPolicy::dispose);
        this.containers.clear();
    }

    @Override
    public String getId() {
        return "drools";
    }

    void update(Policy policy) {
        remove(policy);
        this.containers.put(policy.getId(), new DroolsPolicy(this.ks, policy));
    }

    void remove(Policy policy) {
        DroolsPolicy holder = this.containers.remove(policy.getId());

        if (holder != null) {
            holder.dispose();
        }
    }

    KieContainer getKieContainer(String groupId, String artifactId, String version) {
        return this.ks.newKieContainer(this.ks.newReleaseId(groupId, artifactId, version));
    }
}
