// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.tensor;

import com.google.common.annotations.Beta;
import com.yahoo.tensor.functions.ConstantTensor;
import com.yahoo.tensor.functions.Generate;
import com.yahoo.tensor.functions.Join;
import com.yahoo.tensor.functions.L1Normalize;
import com.yahoo.tensor.functions.L2Normalize;
import com.yahoo.tensor.functions.Matmul;
import com.yahoo.tensor.functions.Reduce;
import com.yahoo.tensor.functions.Rename;
import com.yahoo.tensor.functions.Softmax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * A multidimensional array which can be used in computations.
 * <p>
 * A tensor consists of a set of <i>dimension</i> names and a set of <i>cells</i> containing scalar <i>values</i>.
 * Each cell is is identified by its <i>address</i>, which consists of a set of dimension-label pairs which defines
 * the location of that cell. Both dimensions and labels are string on the form of an identifier or integer.
 * <p>
 * The size of the set of dimensions of a tensor is called its <i>order</i>.
 * <p>
 * In contrast to regular mathematical formulations of tensors, this definition of a tensor allows <i>sparseness</i>
 * as there is no built-in notion of a contiguous space, and even in cases where a space is implied (such as when
 * address labels are integers), there is no requirement that every implied cell has a defined value.
 * Undefined values have no define representation as they are never observed.
 * <p>
 * Tensors can be read and serialized to and from a string form documented in the {@link #toString} method.
 *
 * @author bratseth
 */
@Beta
public interface Tensor {

    TensorType type();

    /** Returns an immutable map of the cells of this */
    Map<TensorAddress, Double> cells();

    /** Returns the value of a cell, or NaN if this cell does not exist/have no value */
    double get(TensorAddress address);

    /** 
     * Returns the value of this as a double if it has no dimensions and one value
     *
     * @throws IllegalStateException if this does not have zero dimensions and one value
     */
    default double asDouble() {
        if (type().dimensions().size() > 0)
            throw new IllegalStateException("This tensor is not dimensionless. Dimensions: " + type().dimensions().size());
        Map<TensorAddress, Double> cells = cells();
        if (cells.size() == 0) return Double.NaN;
        if (cells.size() > 1)
            throw new IllegalStateException("This tensor does not have a single value, it has " + cells().size());
        return cells.values().iterator().next();
    }
    
    // ----------------- Primitive tensor functions
    
    default Tensor map(DoubleUnaryOperator mapper) {
        return new com.yahoo.tensor.functions.Map(new ConstantTensor(this), mapper).evaluate();
    }

    /** Aggregates cells over a set of dimensions, or over all dimensions if no dimensions are specified */
    default Tensor reduce(Reduce.Aggregator aggregator, String ... dimensions) {
        return new Reduce(new ConstantTensor(this), aggregator, Arrays.asList(dimensions)).evaluate();
    }
    /** Aggregates cells over a set of dimensions, or over all dimensions if no dimensions are specified */
    default Tensor reduce(Reduce.Aggregator aggregator, List<String> dimensions) {
        return new Reduce(new ConstantTensor(this), aggregator, dimensions).evaluate();
    }

    default Tensor join(Tensor argument, DoubleBinaryOperator combinator) {
        return new Join(new ConstantTensor(this), new ConstantTensor(argument), combinator).evaluate();
    }

    default Tensor rename(String fromDimension, String toDimension) {
        return new Rename(new ConstantTensor(this), Collections.singletonList(fromDimension), 
                                                    Collections.singletonList(toDimension)).evaluate();
    }

    default Tensor rename(List<String> fromDimensions, List<String> toDimensions) {
        return new Rename(new ConstantTensor(this), fromDimensions, toDimensions).evaluate();
    }
    
    static Tensor from(TensorType type, Function<List<Integer>, Double> valueSupplier) {
        return new Generate(type, valueSupplier).evaluate();
    }
    
    // ----------------- Composite tensor functions which have a defined primitive mapping
    
    default Tensor l1Normalize(String dimension) {
        return new L1Normalize(new ConstantTensor(this), dimension).evaluate();
    }

    default Tensor l2Normalize(String dimension) {
        return new L2Normalize(new ConstantTensor(this), dimension).evaluate();
    }

    default Tensor matmul(Tensor argument, String dimension) {
        return new Matmul(new ConstantTensor(this), new ConstantTensor(argument), dimension).evaluate();
    }

    default Tensor softmax(String dimension) {
        return new Softmax(new ConstantTensor(this), dimension).evaluate();
    }

    // ----------------- Composite tensor functions mapped to primitives here on the fly

    default Tensor multiply(Tensor argument) { return join(argument, (a, b) -> (a * b )); }
    default Tensor add(Tensor argument) { return join(argument, (a, b) -> (a + b )); }
    default Tensor divide(Tensor argument) { return join(argument, (a, b) -> (a / b )); }
    default Tensor subtract(Tensor argument) { return join(argument, (a, b) -> (a - b )); }
    default Tensor max(Tensor argument) { return join(argument, (a, b) -> (a > b ? a : b )); }
    default Tensor min(Tensor argument) { return join(argument, (a, b) -> (a < b ? a : b )); }
    default Tensor atan2(Tensor argument) { return join(argument, Math::atan2); }
    default Tensor larger(Tensor argument) { return join(argument, (a, b) -> ( a > b ? 1.0 : 0.0)); }
    default Tensor largerOrEqual(Tensor argument) { return join(argument, (a, b) -> ( a >= b ? 1.0 : 0.0)); }
    default Tensor smaller(Tensor argument) { return join(argument, (a, b) -> ( a < b ? 1.0 : 0.0)); }
    default Tensor smallerOrEqual(Tensor argument) { return join(argument, (a, b) -> ( a <= b ? 1.0 : 0.0)); }
    default Tensor equal(Tensor argument) { return join(argument, (a, b) -> ( a == b ? 1.0 : 0.0)); }
    default Tensor notEqual(Tensor argument) { return join(argument, (a, b) -> ( a != b ? 1.0 : 0.0)); }

    default Tensor avg(List<String> dimensions) { return reduce(Reduce.Aggregator.avg, dimensions); }
    default Tensor count(List<String> dimensions) { return reduce(Reduce.Aggregator.count, dimensions); }
    default Tensor max(List<String> dimensions) { return reduce(Reduce.Aggregator.max, dimensions); }
    default Tensor min(List<String> dimensions) { return reduce(Reduce.Aggregator.min, dimensions); }
    default Tensor prod(List<String> dimensions) { return reduce(Reduce.Aggregator.prod, dimensions); }
    default Tensor sum(List<String> dimensions) { return reduce(Reduce.Aggregator.sum, dimensions); }

    // ----------------- serialization

    /**
     * Returns this tensor on the form
     * <code>{address1:value1,address2:value2,...}</code>
     * where each address is on the form <code>{dimension1:label1,dimension2:label2,...}</code>,
     * and values are numbers.
     * <p>
     * Cells are listed in the natural order of tensor addresses: Increasing size primarily
     * and by element lexical order secondarily.
     * <p>
     * Note that while this is suggestive of JSON, it is not JSON.
     */
    @Override
    String toString();

    /**
     * Call this from toString in implementations to return the standard string format.
     * (toString cannot be a default method because default methods cannot override super methods).
     *
     * @param tensor the tensor to return the standard string format of
     * @return the tensor on the standard string format
     */
    static String toStandardString(Tensor tensor) {
        if (tensor.cells().isEmpty() && ! tensor.type().dimensions().isEmpty()) // explicitly output type TODO: Never do that?
            return tensor.type() + ":" + contentToString(tensor);
        else
            return contentToString(tensor);
    }

    static String contentToString(Tensor tensor) {
        List<java.util.Map.Entry<TensorAddress, Double>> cellEntries = new ArrayList<>(tensor.cells().entrySet());
        if (tensor.type().dimensions().isEmpty()) { // TODO: Decide on one way to represent degeneration to number
            if (cellEntries.isEmpty()) return "{}";
            double value = cellEntries.get(0).getValue();
            return value == 0.0 ? "{}" : "{" + value +"}";
        }
        
        Collections.sort(cellEntries, java.util.Map.Entry.<TensorAddress, Double>comparingByKey());

        StringBuilder b = new StringBuilder("{");
        for (java.util.Map.Entry<TensorAddress, Double> cell : cellEntries) {
            b.append(cell.getKey().toString(tensor.type())).append(":").append(cell.getValue());
            b.append(",");
        }
        if (b.length() > 1)
            b.setLength(b.length() - 1);
        b.append("}");
        return b.toString();
    }

    // ----------------- equality

    /**
     * Returns true if the given tensor is mathematically equal to this:
     * Both are of type Tensor and have the same content.
     */
    @Override
    boolean equals(Object o);

    /** Returns true if the two given tensors are mathematically equivalent, that is whether both have the same content */
    static boolean equals(Tensor a, Tensor b) {
        if (a == b) return true;
        if ( ! a.cells().equals(b.cells())) return false;
        return true;
    }

    // ----------------- Factories

    /**
     * Returns a tensor instance containing the given data on the standard string format returned by toString
     *
     * @param type the type of the tensor to return
     * @param tensorString the tensor on the standard tensor string format
     */
    static Tensor from(TensorType type, String tensorString) {
        return from(tensorString, Optional.of(type));
    }

    /**
     * Returns a tensor instance containing the given data on the standard string format returned by toString
     *
     * @param tensorType the type of the tensor to return, as a string on the tensor type format, given in
     *        {@link TensorType#fromSpec}
     * @param tensorString the tensor on the standard tensor string format
     */
    static Tensor from(String tensorType, String tensorString) {
        return from(tensorString, Optional.of(TensorType.fromSpec(tensorType)));
    }

    /**
     * Returns a tensor instance containing the given data on the standard string format returned by toString.
     * If a type is not specified it is derived from the first cell of the tensor
     */
    static Tensor from(String tensorString) {
        return from(tensorString, Optional.empty());
    }
    
    static Tensor from(String tensorString, Optional<TensorType> type) {
        tensorString = tensorString.trim();
        try {
            if (tensorString.startsWith("tensor(")) {
                int colonIndex = tensorString.indexOf(':');
                String typeString = tensorString.substring(0, colonIndex);
                String valueString = tensorString.substring(colonIndex + 1);
                TensorType typeFromString = TensorTypeParser.fromSpec(typeString);
                if (type.isPresent() && ! type.get().equals(typeFromString))
                    throw new IllegalArgumentException("Got tensor with type string '" + typeString + "', but was " +
                                                       "passed type " + type);
                return fromValueString(valueString, typeFromString);
            }
            else if (tensorString.startsWith("{")) {
                return fromValueString(tensorString, type.orElse(typeFromValueString(tensorString)));
            }
            else {
                if (type.isPresent() && ! type.get().equals(TensorType.empty))
                    throw new IllegalArgumentException("Got zero-dimensional tensor '" + tensorString + 
                                                       "but type is not empty but " + type.get());
                return IndexedTensor.Builder.of(TensorType.empty).cell(Double.parseDouble(tensorString)).build();
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Excepted a number or a string starting by { or tensor(, got '" +
                                               tensorString + "'");
        }
    }

    static Tensor fromValueString(String tensorCellString, TensorType type) {
        boolean containsIndexedDimensions = type.dimensions().stream().anyMatch(d -> d.isIndexed());
        boolean containsMappedDimensions = type.dimensions().stream().anyMatch(d -> !d.isIndexed());
        if (containsIndexedDimensions && containsMappedDimensions)
            throw new IllegalArgumentException("Mixed dimension types are not supported, got: " + type);
        if (containsMappedDimensions)
            return MappedTensor.from(type, tensorCellString);
        else // indexed or none
            return IndexedTensor.from(type, tensorCellString);
    }

    /** Derive the tensor type from the first address string in the given tensor string */
    static TensorType typeFromValueString(String s) {
        s = s.substring(1).trim(); // remove tensor start
        int firstKeyOrTensorEnd = s.indexOf('}');
        String addressBody = s.substring(0, firstKeyOrTensorEnd).trim();
        if (addressBody.isEmpty()) return TensorType.empty; // Empty tensor
        if ( ! addressBody.startsWith("{")) return TensorType.empty; // Single value tensor

        addressBody = addressBody.substring(1); // remove key start
        if (addressBody.isEmpty()) return TensorType.empty; // Empty key

        TensorType.Builder builder = new TensorType.Builder();
        for (String elementString : addressBody.split(",")) {
            String[] pair = elementString.split(":");
            if (pair.length != 2)
                throw new IllegalArgumentException("Expecting argument elements to be on the form dimension:label, " +
                                                   "got '" + elementString + "'");
            builder.mapped(pair[0].trim());
        }

        return builder.build();
    }

    interface Builder {
        
        /** Creates a suitable builder for the given type */
        static Builder of(TensorType type) {
            boolean containsIndexed = type.dimensions().stream().anyMatch(d -> d.isIndexed());
            boolean containsMapped = type.dimensions().stream().anyMatch( d ->  ! d.isIndexed());
            if (containsIndexed && containsMapped)
                throw new IllegalArgumentException("Combining indexed and mapped dimensions is not supported yet");
            if (containsMapped)
                return new MappedTensor.Builder(type);
            else // indexed or empty
                return IndexedTensor.Builder.of(type);
        }
        
        /** Return a cell builder */
        CellBuilder cell();

        /** Add a built cell */
        Builder cell(TensorAddress address, double value);

        Tensor build();

        interface CellBuilder {

            CellBuilder label(String dimension, String label);

            CellBuilder label(String dimension, int label);

            Builder value(double cellValue);

        }

    }
    
}
