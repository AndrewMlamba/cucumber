require 'cucumber/cucumber_expressions/cucumber_expression_generator'
require 'cucumber/cucumber_expressions/parameter'
require 'cucumber/cucumber_expressions/parameter_registry'

module Cucumber
  module CucumberExpressions
    describe CucumberExpressionGenerator do
      class Currency
      end

      before do
        @parameter_registry = ParameterRegistry.new
        @generator = CucumberExpressionGenerator.new(@parameter_registry)
      end

      it "documents expression generation" do
        parameter_registry = ParameterRegistry.new
        ### [generate-expression]
        generator = CucumberExpressionGenerator.new(parameter_registry)
        undefined_step_text = "I have 2 cucumbers and 1.5 tomato"
        generated_expression = generator.generate_expression(undefined_step_text)
        expect(generated_expression.source).to eq("I have {int} cucumbers and {float} tomato")
        expect(generated_expression.parameters[1].type).to eq(Float)
        ### [generate-expression]
      end

      it "generates expression for no args" do
        assert_expression("hello", [], "hello")
      end

      it "generates expression for int float arg" do
        assert_expression(
          "I have {int} cukes and {float} euro", ["int", "float"],
          "I have 2 cukes and 1.5 euro")
      end

      it "generates expression for just int" do
        assert_expression(
          "{int}", ["int"],
          "99999")
      end

      it "numbers only second argument when builtin type is not reserved keyword" do
        assert_expression(
          "I have {int} cukes and {int} euro", ["int", "int2"],
          "I have 2 cukes and 5 euro")
      end

      it "numbers only second argument when type is not reserved keyword" do
        @parameter_registry.add_parameter(Parameter.new(
          'currency',
          Currency,
          '[A-Z]{3}',
          nil
        ))

        assert_expression(
          "I have a {currency} account and a {currency} account", ["currency", "currency2"],
          "I have a EUR account and a GBP account")
      end

      it "exposes parameters in generated expression" do
        expression = @generator.generate_expression("I have 2 cukes and 1.5 euro")
        types = expression.parameters.map(&:type)
        expect(types).to eq([Integer, Float])
      end

      def assert_expression(expected_expression, expected_argument_names, text)
        generated_expression = @generator.generate_expression(text)
        expect(generated_expression.parameter_names).to eq(expected_argument_names)
        expect(generated_expression.source).to eq(expected_expression)
      end
    end
  end
end
