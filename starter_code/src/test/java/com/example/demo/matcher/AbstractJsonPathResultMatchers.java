package com.example.demo.matcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractJsonPathResultMatchers<F extends Enum<F>, DTO> extends JsonPathResultMatchers {

    String expression;
    Object[] args;
    List<F> excludes;
    Class<DTO> cls;

    public AbstractJsonPathResultMatchers(List<F> excludes, Class<DTO> cls, String expression, Object... args) {
        super(expression, args);
        this.excludes = excludes;
        this.cls = cls;
        this.expression = expression;
        this.args = args;
    }

    @Override
    public ResultMatcher value(Object expectedValue) {
        ObjectMapper mapper = registerMapperModules(new ObjectMapper());
        ResultMatcher matcher = null;

        if (cls.isInstance(expectedValue)) {
            matcher = (result) -> {
                DTO expected = cls.cast(expectedValue);

                // https://stackoverflow.com/a/46886434
                DTO actual = mapper.readValue(result.getResponse().getContentAsString(), getTypeReference());

                assertDto(expected, actual);
            };
        } else if (expectedValue instanceof List) {
            matcher = (result) -> {
                List<DTO> expected = ((List<?>) expectedValue).stream()
                        .filter(cls::isInstance)
                        .map(cls::cast)
                        .collect(Collectors.toList());
                assertEquals(expected.size(), ((List<?>) expectedValue).size(), "Expected array contains unrecognised classes");

                List<DTO> actual = mapper.readValue(result.getResponse().getContentAsString(), getListTypeReference());

                assertEquals(expected.size(), actual.size(), "Expected and actual array size mismatch");

                for (int i = 0; i < expected.size(); i++) {
                    assertDto(expected.get(i), actual.get(i));
                }
            };
        } else {
            fail("Unexpected object class " + expectedValue.getClass().getSimpleName());
        }
        return matcher;
    }

    /**
     * Get TypeReference for object
     * The standard 'mapper.readValue(contentAsString, new TypeReference<>() {})' does not work with generics
     * @return
     */
    protected abstract TypeReference<DTO> getTypeReference();

    protected abstract TypeReference<List<DTO>> getListTypeReference();

    protected abstract void assertDto(DTO expected, DTO actual);

    protected ObjectMapper registerMapperModules(ObjectMapper mapper) {
        return mapper;
    }

    protected String path(String child) {
        return expression + "." + child;
    }
}
