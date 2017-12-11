package edu.stanford.taddair.DecentralizedSGD

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.numerics.sigmoid
import edu.stanford.taddair.DecentralizedSGD.actors.centralized.{OutputActor, ParameterShard}
import edu.stanford.taddair.DecentralizedSGD.actors.centralized.DataShard.FetchParameters
import edu.stanford.taddair.DecentralizedSGD.actors.centralized.Layer.{ForwardPass, MyChild}
import edu.stanford.taddair.DecentralizedSGD.actors.centralized.{Layer, OutputActor, ParameterShard}
import org.scalatest.{MustMatchers, WordSpecLike}


class LayerTest extends TestKit(ActorSystem("testSystem")) with WordSpecLike with MustMatchers {

  "Layer actor" must {

    val numLayers = 2

    //parameter shard associate with this layer
    val parameterShardTestActor1 = TestActorRef(new ParameterShard(
      1,
      0.1,
      DenseMatrix((0.341232, 0.129952, -0.923123)
        , (-0.115223, 0.570345, -0.328932))))

    val parameterShardTestActor2 = TestActorRef(new ParameterShard(
      1,
      0.1,
      DenseMatrix((-0.993423, 0.164732, 0.752621))))

    val outputTestActor = TestActorRef(new OutputActor)

    val layers: Array[TestActorRef[Layer]] = new Array[TestActorRef[Layer]](numLayers)

    for (l <- 0 to numLayers - 1) {

      layers(l) = TestActorRef(new Layer(
        0
        , l
        , (x: DenseVector[Double]) => x.map(el => sigmoid(el))
        , (x: DenseVector[Double]) => x.map(el => sigmoid(el) * (1 - sigmoid(el)))
        , if (l > 0) Some(layers(l - 1)) else None //parent layer actor
        , if (l == 0) parameterShardTestActor1 else parameterShardTestActor2
        , if (l == numLayers - 1) Some(outputTestActor) else None)) //layer needs access to its parameter shard to read from and update

      if (l > 0) layers(l - 1) ! MyChild(layers(l)) //tell last layer what its child is
    }


    val layer1 = layers(0)

    val layer2 = layers(1)


    "fetch the correct parameters from its parameter shard" in {

      //ask layer actor to update its weight parameter from its associated parameter shard
      layer1 ! FetchParameters

      layer1.underlyingActor.latestWeights must equal(DenseMatrix((0.341232, 0.129952, -0.923123)
        , (-0.115223, 0.570345, -0.328932)))
    }


    "correctly pass result to child layer actor" in {

      layer1 ! FetchParameters

      layer2 ! FetchParameters

      layer1 ! ForwardPass(DenseVector(1.0, 0.0, 0.0), DenseVector(0.0))

      layer1.underlyingActor.activations must equal(DenseVector(1.0, 0.0, 0.0))
      layer2.underlyingActor.activations must equal(DenseVector(1.0, 0.341232, -0.115223))

    }

    "compute the correct output prediction" in {

      layer1 ! FetchParameters

      layer2 ! FetchParameters

      layer1 ! ForwardPass(DenseVector(1.0, 0.0, 0.0), DenseVector(0.0))

      outputTestActor.underlyingActor.latestOutputs(0) must equal(DenseVector(0.3676098854895219))

    }


    "compute the correct weight updates from gradients" in {

      layer1 ! FetchParameters

      layer2 ! FetchParameters

      layer1 ! ForwardPass(DenseVector(1.0, 0.0, 0.0), DenseVector(0.0))

      parameterShardTestActor1.underlyingActor.latestParameter must equal(
        DenseMatrix((0.3408901024055906, 0.129952, -0.923123)
          , (-0.11682563680703478, 0.570345, -0.328932)))

      parameterShardTestActor2.underlyingActor.latestParameter must equal(
        DenseMatrix((-1.001968932055437, 0.15973699022915824, 0.7485939339604513)))

    }


  }


}
