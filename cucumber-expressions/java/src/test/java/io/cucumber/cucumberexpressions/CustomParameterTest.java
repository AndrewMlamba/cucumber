package io.cucumber.cucumberexpressions;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CustomParameterTest {
    public static class Color {
        public final String name;

        /// [color-constructor]
        public Color(String name) {
            this.name = name;
        }
        /// [color-constructor]

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return ((Color) obj).name.equals(name);
        }
    }

    private ParameterRegistry parameterRegistry = new ParameterRegistry(Locale.ENGLISH);

    @Before
    public void create_parameter() {
        /// [add-color-parameter]
        parameterRegistry.addParameter(new SimpleParameter<>(
                "color",
                Color.class,
                "red|blue|yellow",
                new Function<String, Color>() {
                    @Override
                    public Color apply(String name) {
                        return new Color(name);
                    }
                }
        ));
        /// [add-color-parameter]
    }

    @Test
    public void transforms_CucumberExpression_arguments_with_expression_type() {
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>emptyList(), parameterRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void transforms_CucumberExpression_arguments_with_expression_type_using_optional_group() {
        parameterRegistry = new ParameterRegistry(Locale.ENGLISH);
        parameterRegistry.addParameter(new SimpleParameter<>(
                "color",
                Color.class,
                asList("red|blue|yellow", "(?:dark|light) (?:red|blue|yellow)"),
                new Function<String, Color>() {
                    @Override
                    public Color apply(String name) {
                        return new Color(name);
                    }
                }
        ));
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>emptyList(), parameterRegistry);
        Object transformedArgumentValue = expression.match("I have a dark red ball").get(0).getTransformedValue();
        assertEquals(new Color("dark red"), transformedArgumentValue);
    }

    @Test
    public void transforms_CucumberExpression_arguments_with_explicit_type() {
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>singletonList(Color.class), parameterRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void transforms_CucumberExpression_arguments_using_argument_name_as_type() {
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>emptyList(), parameterRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void transforms_CucumberExpression_arguments_with_explicit_type_using_constructor_directly() {
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>singletonList(Color.class), new ParameterRegistry(Locale.ENGLISH));
        Color transformedArgumentValue = (Color) expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals("red", transformedArgumentValue.name);
    }

    @Test
    public void defers_transformation_until_queried_from_argument() {
        parameterRegistry.addParameter(new SimpleParameter<>(
                "throwing",
                String.class,
                "bad",
                new Function<String, String>() {
                    @Override
                    public String apply(String name) {
                        throw new RuntimeException(String.format("Can't transform [%s]", name));
                    }
                }));
        Expression expression = new CucumberExpression("I have a {throwing} parameter", Collections.<Type>emptyList(), parameterRegistry);
        List<Argument> arguments = expression.match("I have a bad parameter");
        try {
            arguments.get(0).getTransformedValue();
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("Can't transform [bad]", expected.getMessage());
        }
    }

    ///// Conflicting parameter types

    @Test
    public void conflicting_parameter_type_is_detected_for_type() {
        try {
            parameterRegistry.addParameter(new SimpleParameter<>(
                    "color",
                    String.class,
                    ".*",
                    new Function<String, String>() {
                        @Override
                        public String apply(String s) {
                            return s;
                        }
                    }));
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("There is already a parameter with type name color", expected.getMessage());
        }
    }

    @Test
    public void conflicting_parameter_type_is_detected_for_type_name() {
        try {
            parameterRegistry.addParameter(new SimpleParameter<>(
                    "whatever",
                    Color.class,
                    ".*",
                    new Function<String, Color>() {
                        @Override
                        public Color apply(String s) {
                            return new Color(s);
                        }
                    }));
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("There is already a parameter with type io.cucumber.cucumberexpressions.CustomParameterTest$Color", expected.getMessage());
        }
    }

    @Test
    public void conflicting_parameter_type_is_detected_for_regexp() {
        try {
            parameterRegistry.addParameter(new SimpleParameter<>(
                    "whatever",
                    String.class,
                    "red|blue|yellow",
                    new Function<String, String>() {
                        @Override
                        public String apply(String s) {
                            return s;
                        }
                    }));
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("There is already a parameter with regexp red|blue|yellow", expected.getMessage());
        }
    }

    ///// RegularExpression

    @Test
    public void transforms_RegularExpression_arguments_with_explicit_type() {
        Expression expression = new RegularExpression(compile("I have a (red|blue|yellow) ball"), Collections.<Type>singletonList(Color.class), parameterRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void transforms_RegularExpression_arguments_without_explicit_type() {
        Expression expression = new RegularExpression(compile("I have a (red|blue|yellow) ball"), Collections.<Type>emptyList(), parameterRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void transforms_RegularExpression_arguments_with_explicit_type_using_constructor_directly() {
        Expression expression = new RegularExpression(compile("I have a (red|blue|yellow) ball"), Collections.<Type>singletonList(Color.class), new ParameterRegistry(Locale.ENGLISH));
        Color transformedArgumentValue = (Color) expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals("red", transformedArgumentValue.name);
    }
}
