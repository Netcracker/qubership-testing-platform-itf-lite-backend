package org.qubership.atp.itf.lite.backend.utils;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.service.DynamicVariablesService;

@ExtendWith(MockitoExtension.class)
class ParsingUtilsTest {
    private DynamicVariablesService dynamicVariablesService;

    @Test
    public void parseStringOnVariables_HaveManyVariablesInString_setWithVariables() {
        //given
        String string = "any text \\{{$value1}}\\ a_-*7\\{{$second}}\\ \\{{$value3}}\\ \\{{$value4}}\\";
        String string2 = "any text \\{{$value5}}\\ a_-*7\\{{$second}}\\ \\{{$value6}}\\ \\{{$value7}}\\";

        //when
        Set<String> actualSet = dynamicVariablesService.getDynamicVariables(string, string2);

        //then
        Assertions.assertEquals("[{{$value5}}, {{$value6}}, {{$value7}}, {{$value1}}, {{$value3}}, {{$value4}}, {{$second}}]",
                actualSet.toString(), "Not Correctly parse variables");
    }

    @Test
    public void getDomain_haveUrlWithPort_collectDomainCorrectly() {
        //given
        String url = "http://qaapp125.com:6820/ATP/api/v1/dataSets/9163161935513537653";
        String er = "qaapp125.com";
        //when
        String ar = UrlParsingUtils.getDomain(url);
        //then
        Assertions.assertEquals(er, ar, "Not Correctly parse domain");
    }
}
