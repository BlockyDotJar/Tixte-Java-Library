/**
 * Copyright 2022 Dominic (aka. BlockyDotJar)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.errorprone.annotations.CheckReturnValue;
import dev.blocky.library.tixte.api.Domains;
import dev.blocky.library.tixte.api.TixteClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Some basic examples, how to use the {@link Domains domain system}.
 *
 * @author BlockyDotJar
 * @version v1.0.0
 * @since v1.0.0-beta.3
 */
public class DomainExample
{
    /**
     * @throws IOException  If the request could not be executed due to cancellation,
     *                      a connectivity problem or timeout. Because networks can fail during an exchange,
     *                      it is possible that the remote server accepted the request before the failure.
     *
     * @return Adds a subdomain to our other domains.
     */
    @NotNull
    @CheckReturnValue
    public static Domains createDomain() throws IOException
    {
        TixteClient client = BasicTixteClientExample.getTixteClient();
        Domains domains = client.getDomainSystem();

        // Adds a new domain to the dashboard.
        // You cannot use domain endings, you don't own.
        // This mustn't contain http or https in the name.
        // Note that you only can have up to 3 subdomains without a subscription.
        return domains.addSubdomain("i-am.from.space");
    }

    /**
     * @throws IOException  If the request could not be executed due to cancellation,
     *                      a connectivity problem or timeout. Because networks can fail during an exchange,
     *                      it is possible that the remote server accepted the request before the failure.
     *
     * @return Adds a subdomain with random name to our other domains.
     */
    @NotNull
    public static Domains createRandomDomain() throws IOException
    {
        TixteClient client = BasicTixteClientExample.getTixteClient();
        Domains domains = client.getDomainSystem();

        // You should note everything I said above.
        // This will only create the beginning of the domain, because otherwise it could create a domain, you don*t own.
        return domains.addSubdomain(domains.generateDomain() + ".from.space");
    }

    /**
     * @throws IOException  If the request could not be executed due to cancellation,
     *                      a connectivity problem or timeout. Because networks can fail during an exchange,
     *                      it is possible that the remote server accepted the request before the failure.
     *
     * @return Deletes a domain of your domain collection.
     */
    @Nullable
    public static Domains deleteDomain() throws IOException
    {
        TixteClient client = BasicTixteClientExample.getTixteClient();
        Domains domains = client.getDomainSystem();

        // This will delete the domain.
        // Of course, you cannot delete a domain, that doesn't exist
        // This also mustn't contain http or https in the name.
        return domains.deleteDomain("i-am.from.space");
    }
}
