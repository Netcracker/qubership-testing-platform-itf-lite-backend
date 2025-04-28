package org.qubership.atp.itf.lite.backend.model.entities.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;

public class GraphQLComposerTest {

    private final RequestBody REQUEST_BODY_INSTANCE = new RequestBody();
    private final String COMMON_QUERY = "  \n" +
            "  {\n" +
            "  listProductOfferings \n" +
            "  @filter (filters: \n" +
            "        [\"product.chars.Equipment type.defaultValue=Smartphones\",\"product.chars.Equipment model.defaultValue=iPhone 13\",\"product.chars.Brand.defaultValue=Apple\",\"checkStock=outOfStock\"]\n" +
            "  \n" +
            "  )\n" +
            "{\n" +
            "    id\n" +
            "    name\n" +
            "    isRoot    \n" +
            "    product\n" +
            "    {\n" +
            "      code\n" +
            "      description\n" +
            "      id\n" +
            "      name\n" +
            "      originalDescription\n" +
            "      originalName\n" +
            "      stock{\n" +
            "          checkStock\n" +
            "          availableForPreOrder\n" +
            "      }\n" +
            "      chars\n" +
            "      {        \n" +
            "        originalName\n" +
            "        type        \n" +
            "        value        \n" +
            "      }\n" +
            "      params\n" +
            "      {\n" +
            "        businessType\n" +
            "        {\n" +
            "        name\n" +
            "        }\n" +
            "        group\n" +
            "        {\n" +
            "        name\n" +
            "        }\n" +
            "        name\n" +
            "        type\n" +
            "        availableFrom\n" +
            "        availableTo\n" +
            "      }\n" +
            "    }\n" +
            "    isBundle\n" +
            "  }\n" +
            "}";

    @Test
    void convert_NonEmptyQuery_And_NonEmptyVariables_ValidNonEmptyJson() {
        String query = "query listCustomers($offset: Int, $limit: Int, $filter: [String!]) {\n" +
                "  listCustomers\n" +
                "  @filter(filters: $filter)\n" +
                "  @limit(limit: $limit, offset: $offset) {\n" +
                "    id\n" +
                "    id\n" +
                "    name\n" +
                "    customerNumber\n" +
                "    customerSince\n" +
                "    createdBy\n" +
                "    product {\n" +
                "      id\n" +
                "      name\n" +
                "      characteristic {\n" +
                "        value\n" +
                "        name\n" +
                "        __typename\n" +
                "      }\n" +
                "      __typename\n" +
                "    }\n" +
                "    relatedParty {\n" +
                "      atReferredType\n" +
                "      id\n" +
                "      name\n" +
                "      role\n" +
                "      __typename\n" +
                "    }\n" +
                "    engagedParty {\n" +
                "      atReferredType\n" +
                "      id\n" +
                "      name\n" +
                "      origin {\n" +
                "        ... on Individual {\n" +
                "          id\n" +
                "          birthDate\n" +
                "          individualIdentification {\n" +
                "            type\n" +
                "            identificationId\n" +
                "            __typename\n" +
                "          }\n" +
                "          contactMedium {\n" +
                "            type\n" +
                "            characteristic {\n" +
                "              type\n" +
                "              city\n" +
                "              phoneNumber\n" +
                "              emailAddress\n" +
                "              __typename\n" +
                "            }\n" +
                "            __typename\n" +
                "          }\n" +
                "          __typename\n" +
                "        }\n" +
                "        __typename\n" +
                "      }\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}";
        String variables = "{    \"filter\": [\n" +
                "  \"documentId.eq=1122334500\"\n" +
                "],\n" +
                "  \"offset\": 0,\n" +
                "  \"limit\": 20\n" +
                "}";
        String expected = "{\"variables\":{\"filter\":[\"documentId.eq=1122334500\"],\"offset\":0,\"limit\":20},\"query\":\"query listCustomers($offset: Int, $limit: Int, $filter: [String!]) {   listCustomers   @filter(filters: $filter)   @limit(limit: $limit, offset: $offset) {     id     id     name     customerNumber     customerSince     createdBy     product {       id       name       characteristic {         value         name         __typename       }       __typename     }     relatedParty {       atReferredType       id       name       role       __typename     }     engagedParty {       atReferredType       id       name       origin {         ... on Individual {           id           birthDate           individualIdentification {             type             identificationId             __typename           }           contactMedium {             type             characteristic {               type               city               phoneNumber               emailAddress               __typename             }             __typename           }           __typename         }         __typename       }       __typename     }     __typename   } }\"}";
        String actual = REQUEST_BODY_INSTANCE.composeGraphQlBody(query, variables);
        assertEquals(expected, actual);
    }

    @Test
    void convert_NonEmptyQuery_And_NonEmptyVariables_EmptyJson() {
        String variables = "{}";
        String expected = "{\"variables\":{},\"query\":\"     {   listProductOfferings    @filter (filters:          [\\\"product.chars.Equipment type.defaultValue=Smartphones\\\",\\\"product.chars.Equipment model.defaultValue=iPhone 13\\\",\\\"product.chars.Brand.defaultValue=Apple\\\",\\\"checkStock=outOfStock\\\"]      ) {     id     name     isRoot         product     {       code       description       id       name       originalDescription       originalName       stock{           checkStock           availableForPreOrder       }       chars       {                 originalName         type                 value               }       params       {         businessType         {         name         }         group         {         name         }         name         type         availableFrom         availableTo       }     }     isBundle   } }\"}";
        String actual = REQUEST_BODY_INSTANCE.composeGraphQlBody(COMMON_QUERY, variables);
        assertEquals(expected, actual);
    }

    @Test
    void convert_NonEmptyQuery_And_EmptyVariables() {
        String variables = "";
        String expected = "{\"variables\":{},\"query\":\"     {   listProductOfferings    @filter (filters:          [\\\"product.chars.Equipment type.defaultValue=Smartphones\\\",\\\"product.chars.Equipment model.defaultValue=iPhone 13\\\",\\\"product.chars.Brand.defaultValue=Apple\\\",\\\"checkStock=outOfStock\\\"]      ) {     id     name     isRoot         product     {       code       description       id       name       originalDescription       originalName       stock{           checkStock           availableForPreOrder       }       chars       {                 originalName         type                 value               }       params       {         businessType         {         name         }         group         {         name         }         name         type         availableFrom         availableTo       }     }     isBundle   } }\"}";
        String actual = REQUEST_BODY_INSTANCE.composeGraphQlBody(COMMON_QUERY, variables);
        assertEquals(expected, actual);
    }

    @Test
    void convert_NonEmptyQuery_And_NullVariables() {
        String variables = null;
        String expected = "{\"variables\":{},\"query\":\"     {   listProductOfferings    @filter (filters:          [\\\"product.chars.Equipment type.defaultValue=Smartphones\\\",\\\"product.chars.Equipment model.defaultValue=iPhone 13\\\",\\\"product.chars.Brand.defaultValue=Apple\\\",\\\"checkStock=outOfStock\\\"]      ) {     id     name     isRoot         product     {       code       description       id       name       originalDescription       originalName       stock{           checkStock           availableForPreOrder       }       chars       {                 originalName         type                 value               }       params       {         businessType         {         name         }         group         {         name         }         name         type         availableFrom         availableTo       }     }     isBundle   } }\"}";
        String actual = REQUEST_BODY_INSTANCE.composeGraphQlBody(COMMON_QUERY, variables);
        assertEquals(expected, actual);
    }

    @Test
    void convert_NonEmptyQuery_And_NonEmptyVariables_EffectivelyEmptyJsonWithWhitespaces() {
        String variables = " \n { \n \n \n\n } \n\n \n";
        String expected = "{\"variables\":{},\"query\":\"     {   listProductOfferings    @filter (filters:          [\\\"product.chars.Equipment type.defaultValue=Smartphones\\\",\\\"product.chars.Equipment model.defaultValue=iPhone 13\\\",\\\"product.chars.Brand.defaultValue=Apple\\\",\\\"checkStock=outOfStock\\\"]      ) {     id     name     isRoot         product     {       code       description       id       name       originalDescription       originalName       stock{           checkStock           availableForPreOrder       }       chars       {                 originalName         type                 value               }       params       {         businessType         {         name         }         group         {         name         }         name         type         availableFrom         availableTo       }     }     isBundle   } }\"}";
        String actual = REQUEST_BODY_INSTANCE.composeGraphQlBody(COMMON_QUERY, variables);
        assertEquals(expected, actual);
    }

    @Test
    void convert_NullQuery_And_AnyValidVariables() {
        String variables = "{    \"filter\": [\n" +
                "  \"documentId.eq=1122334500\"\n" +
                "],\n" +
                "  \"offset\": 0,\n" +
                "  \"limit\": 20\n" +
                "}";
        String expected = "{\"variables\":{\"filter\":[\"documentId.eq=1122334500\"],\"offset\":0,\"limit\":20},\"query\":\"\"}";
        String actual = REQUEST_BODY_INSTANCE.composeGraphQlBody(null, variables);
        assertEquals(expected, actual);
    }
}
