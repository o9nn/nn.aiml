package org.ninelym.cognitive.hypergraph

import org.junit.Test
import kotlin.test.*

/**
 * Comprehensive unit tests for Hypergraph operations
 */
class HypergraphTest {
    
    @Test
    fun `test addAtom adds new atom successfully`() {
        val hypergraph = Hypergraph()
        val atom = Atom(
            id = "test_atom_1",
            type = AtomType.CONCEPT,
            name = "test_concept",
            truthValue = TruthValue.DEFAULT,
            attentionValue = AttentionValue.DEFAULT
        )
        
        val result = hypergraph.addAtom(atom)
        
        assertTrue(result, "Atom should be added successfully")
        assertNotNull(hypergraph.getAtom("test_atom_1"))
    }
    
    @Test
    fun `test addAtom rejects duplicate atoms`() {
        val hypergraph = Hypergraph()
        val atom = Atom(
            id = "duplicate_atom",
            type = AtomType.CONCEPT,
            name = "duplicate",
            truthValue = TruthValue.DEFAULT,
            attentionValue = AttentionValue.DEFAULT
        )
        
        val firstAdd = hypergraph.addAtom(atom)
        val secondAdd = hypergraph.addAtom(atom)
        
        assertTrue(firstAdd, "First add should succeed")
        assertFalse(secondAdd, "Second add should fail")
    }
    
    @Test
    fun `test removeAtom removes existing atom`() {
        val hypergraph = Hypergraph()
        val atom = Atom(
            id = "removable_atom",
            type = AtomType.CONCEPT,
            name = "removable",
            truthValue = TruthValue.DEFAULT,
            attentionValue = AttentionValue.DEFAULT
        )
        
        hypergraph.addAtom(atom)
        val removed = hypergraph.removeAtom("removable_atom")
        
        assertTrue(removed, "Atom should be removed successfully")
        assertNull(hypergraph.getAtom("removable_atom"))
    }
    
    @Test
    fun `test getAtomsByType filters correctly`() {
        val hypergraph = Hypergraph()
        
        // Add different types of atoms
        hypergraph.addAtom(Atom("concept_1", AtomType.CONCEPT, "concept1"))
        hypergraph.addAtom(Atom("concept_2", AtomType.CONCEPT, "concept2"))
        hypergraph.addAtom(Atom("predicate_1", AtomType.PREDICATE, "predicate1"))
        
        val concepts = hypergraph.getAtomsByType(AtomType.CONCEPT)
        val predicates = hypergraph.getAtomsByType(AtomType.PREDICATE)
        
        assertEquals(2, concepts.size, "Should find 2 concepts")
        assertEquals(1, predicates.size, "Should find 1 predicate")
    }
    
    @Test
    fun `test addLink creates valid hyperlink`() {
        val hypergraph = Hypergraph()
        
        // Add atoms first
        hypergraph.addAtom(Atom("atom_1", AtomType.CONCEPT, "atom1"))
        hypergraph.addAtom(Atom("atom_2", AtomType.CONCEPT, "atom2"))
        
        val link = HyperLink(
            id = "link_1",
            type = LinkType.SIMILARITY,
            targets = listOf("atom_1", "atom_2"),
            strength = 0.8f
        )
        
        val result = hypergraph.addLink(link)
        
        assertTrue(result, "Link should be added successfully")
    }
    
    @Test
    fun `test addLink fails for non-existent atoms`() {
        val hypergraph = Hypergraph()
        
        val link = HyperLink(
            id = "invalid_link",
            type = LinkType.SIMILARITY,
            targets = listOf("non_existent_1", "non_existent_2"),
            strength = 0.8f
        )
        
        val result = hypergraph.addLink(link)
        
        assertFalse(result, "Link should fail for non-existent atoms")
    }
    
    @Test
    fun `test getConnectedAtoms returns connected atoms`() {
        val hypergraph = Hypergraph()
        
        // Create a small network
        hypergraph.addAtom(Atom("center", AtomType.CONCEPT, "center"))
        hypergraph.addAtom(Atom("connected_1", AtomType.CONCEPT, "connected1"))
        hypergraph.addAtom(Atom("connected_2", AtomType.CONCEPT, "connected2"))
        
        hypergraph.addLink(HyperLink("link_1", LinkType.SIMILARITY, listOf("center", "connected_1")))
        hypergraph.addLink(HyperLink("link_2", LinkType.SIMILARITY, listOf("center", "connected_2")))
        
        val connected = hypergraph.getConnectedAtoms("center")
        
        assertEquals(2, connected.size, "Should find 2 connected atoms")
    }
    
    @Test
    fun `test updateAtomAttention updates attention value`() {
        val hypergraph = Hypergraph()
        val atom = Atom(
            id = "attention_test",
            type = AtomType.CONCEPT,
            name = "attention",
            attentionValue = AttentionValue(0.5f, 0.5f)
        )
        
        hypergraph.addAtom(atom)
        
        val newAttention = AttentionValue(0.9f, 0.8f)
        val updated = hypergraph.updateAtomAttention("attention_test", newAttention)
        
        assertTrue(updated, "Attention should be updated")
        
        val retrievedAtom = hypergraph.getAtom("attention_test")
        assertEquals(0.9f, retrievedAtom?.attentionValue?.sti)
        assertEquals(0.8f, retrievedAtom?.attentionValue?.lti)
    }
    
    @Test
    fun `test getActiveTensors filters by attention`() {
        val hypergraph = Hypergraph()
        
        // Add atoms with different attention values
        hypergraph.addAtom(Atom(
            "high_attention",
            AtomType.CONCEPT,
            "high",
            attentionValue = AttentionValue(0.8f, 0.7f)
        ))
        
        hypergraph.addAtom(Atom(
            "low_attention",
            AtomType.CONCEPT,
            "low",
            attentionValue = AttentionValue(0.1f, 0.1f)
        ))
        
        val activeTensors = hypergraph.getActiveTensors(minAttention = 1.0f)
        
        assertEquals(1, activeTensors.size, "Should find 1 high-attention atom")
    }
    
    @Test
    fun `test getStats returns accurate statistics`() {
        val hypergraph = Hypergraph()
        
        // Add various atoms
        repeat(5) { i ->
            hypergraph.addAtom(Atom(
                "atom_$i",
                AtomType.CONCEPT,
                "concept_$i",
                attentionValue = AttentionValue(0.5f, 0.5f)
            ))
        }
        
        hypergraph.addAtom(Atom("link_atom", AtomType.LINK, "link"))
        
        val stats = hypergraph.getStats()
        
        assertEquals(6, stats.atomCount, "Should have 6 atoms")
        assertEquals(5, stats.typeDistribution[AtomType.CONCEPT])
        assertEquals(1, stats.typeDistribution[AtomType.LINK])
    }
    
    @Test
    fun `test performActivationSpreading spreads activation`() {
        val hypergraph = Hypergraph()
        
        // Create a chain of atoms
        hypergraph.addAtom(Atom(
            "source",
            AtomType.CONCEPT,
            "source",
            attentionValue = AttentionValue(1.0f, 0.5f)
        ))
        
        hypergraph.addAtom(Atom(
            "target1",
            AtomType.CONCEPT,
            "target1",
            attentionValue = AttentionValue(0.2f, 0.2f)
        ))
        
        hypergraph.addAtom(Atom(
            "target2",
            AtomType.CONCEPT,
            "target2",
            attentionValue = AttentionValue(0.2f, 0.2f)
        ))
        
        hypergraph.addLink(HyperLink("link1", LinkType.SIMILARITY, listOf("source", "target1")))
        hypergraph.addLink(HyperLink("link2", LinkType.SIMILARITY, listOf("target1", "target2")))
        
        val result = hypergraph.performActivationSpreading(
            initialAtoms = listOf("source"),
            spreadingStrength = 0.8f,
            maxDepth = 2
        )
        
        assertTrue(result.activatedAtoms > 1, "Should activate multiple atoms")
        assertTrue(result.totalActivation > 0.0f, "Should have positive total activation")
    }
    
    @Test
    fun `test getMeshConnectivity identifies clusters`() {
        val hypergraph = Hypergraph()
        
        // Create a cluster of high-attention atoms
        val highAttention = AttentionValue(0.8f, 0.7f)
        
        hypergraph.addAtom(Atom("cluster1", AtomType.CONCEPT, "c1", attentionValue = highAttention))
        hypergraph.addAtom(Atom("cluster2", AtomType.CONCEPT, "c2", attentionValue = highAttention))
        hypergraph.addAtom(Atom("cluster3", AtomType.CONCEPT, "c3", attentionValue = highAttention))
        
        hypergraph.addLink(HyperLink("link1", LinkType.SIMILARITY, listOf("cluster1", "cluster2")))
        hypergraph.addLink(HyperLink("link2", LinkType.SIMILARITY, listOf("cluster2", "cluster3")))
        
        val connectivity = hypergraph.getMeshConnectivity(minAttention = 1.0f)
        
        assertEquals(3, connectivity.totalNodes, "Should find 3 high-attention nodes")
        assertTrue(connectivity.totalEdges > 0, "Should have edges")
    }
    
    @Test
    fun `test TruthValue validation`() {
        assertTrue(TruthValue(0.5f, 0.5f).isValid())
        assertTrue(TruthValue(0.0f, 1.0f).isValid())
        assertTrue(TruthValue(1.0f, 0.0f).isValid())
        assertFalse(TruthValue(-0.1f, 0.5f).isValid())
        assertFalse(TruthValue(0.5f, 1.1f).isValid())
    }
    
    @Test
    fun `test AttentionValue total importance`() {
        val attention = AttentionValue(0.6f, 0.4f)
        assertEquals(1.0f, attention.totalImportance())
        
        val highAttention = AttentionValue(0.9f, 0.8f)
        assertEquals(1.7f, highAttention.totalImportance())
    }
    
    @Test
    fun `test Atom to CognitiveTensor conversion`() {
        val atom = Atom(
            id = "tensor_test",
            type = AtomType.CONCEPT,
            name = "tensor",
            truthValue = TruthValue(0.8f, 0.9f),
            attentionValue = AttentionValue(0.7f, 0.6f)
        )
        
        val tensor = atom.toCognitiveTensor()
        
        assertNotNull(tensor)
        assertEquals(AtomType.CONCEPT.modalityWeight, tensor.modality)
        assertEquals(0.9f, tensor.confidence)
        assertEquals(0.7f, tensor.salience)
    }
}
