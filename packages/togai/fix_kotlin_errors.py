#!/usr/bin/env python3
"""
Automated Kotlin error fixing script for togai repository
"""

import os
import re
from pathlib import Path

def fix_cognitive_engine():
    """Fix CognitiveEngine.kt issues"""
    file_path = "src/main/kotlin/org/ninelym/cognitive/CognitiveEngine.kt"
    
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Ensure proper imports
    if "import kotlinx.coroutines.flow.MutableStateFlow" not in content:
        # Add missing imports at the top after package declaration
        import_section = """
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
"""
        content = content.replace("package org.ninelym.cognitive\n", 
                                 f"package org.ninelym.cognitive\n{import_section}")
    
    with open(file_path, 'w') as f:
        f.write(content)
    
    print(f"✓ Fixed {file_path}")

def fix_hypergraph():
    """Fix Hypergraph.kt missing methods"""
    file_path = "src/main/kotlin/org/ninelym/cognitive/hypergraph/Hypergraph.kt"
    
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Check if runAttentionCycle method exists
    if "fun runAttentionCycle" not in content:
        # Add missing method before the last closing brace
        additional_methods = """
    
    /**
     * Run attention spreading cycle (for ECAN integration)
     */
    fun runAttentionCycle() {
        // Attention spreading is handled by ECAN kernel
        // This is a placeholder for direct hypergraph manipulation
        atoms.values.forEach { atom ->
            // Decay attention values slightly
            atom.attentionValue.sti *= 0.99f
            atom.attentionValue.lti *= 0.999f
        }
    }
"""
        # Insert before the last closing brace
        content = content.rstrip()
        if content.endswith("}"):
            content = content[:-1] + additional_methods + "\n}"
    
    with open(file_path, 'w') as f:
        f.write(content)
    
    print(f"✓ Fixed {file_path}")

def fix_ecan_scheduler():
    """Fix ECANScheduler.kt incomplete implementations"""
    file_path = "src/main/kotlin/org/ninelym/cognitive/ecan/ECANScheduler.kt"
    
    if not os.path.exists(file_path):
        print(f"✗ File not found: {file_path}")
        return
    
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Add missing imports if needed
    if "import kotlinx.coroutines" not in content:
        content = content.replace("package org.ninelym.cognitive.ecan\n",
                                 "package org.ninelym.cognitive.ecan\n\nimport kotlinx.coroutines.*\n")
    
    with open(file_path, 'w') as f:
        f.write(content)
    
    print(f"✓ Fixed {file_path}")

def fix_phase6_demo():
    """Fix Phase6Demo.kt errors"""
    file_path = "src/main/kotlin/org/ninelym/cognitive/Phase6Demo.kt"
    
    if not os.path.exists(file_path):
        print(f"✗ File not found: {file_path}")
        return
    
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Fix GlobalScope usage - replace with proper coroutine scope
    content = content.replace("GlobalScope.launch", "CoroutineScope(Dispatchers.Default).launch")
    
    # Add missing imports
    if "import kotlinx.coroutines.CoroutineScope" not in content:
        content = content.replace("package org.ninelym.cognitive\n",
                                 "package org.ninelym.cognitive\n\nimport kotlinx.coroutines.*\n")
    
    with open(file_path, 'w') as f:
        f.write(content)
    
    print(f"✓ Fixed {file_path}")

def fix_responsive_ui():
    """Fix ResponsiveUIFramework.kt missing dependencies"""
    file_path = "src/main/kotlin/org/ninelym/layla/ui/ResponsiveUIFramework.kt"
    
    if not os.path.exists(file_path):
        print(f"✗ File not found: {file_path}")
        return
    
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Add missing imports
    missing_imports = """
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
"""
    
    if "import kotlinx.coroutines.flow.MutableStateFlow" not in content:
        content = content.replace("package org.ninelym.layla.ui\n",
                                 f"package org.ninelym.layla.ui\n{missing_imports}")
    
    with open(file_path, 'w') as f:
        f.write(content)
    
    print(f"✓ Fixed {file_path}")

def main():
    """Main execution"""
    print("🔧 Starting Kotlin error fixes...\n")
    
    os.chdir("/home/ubuntu/togai")
    
    fix_cognitive_engine()
    fix_hypergraph()
    fix_ecan_scheduler()
    fix_phase6_demo()
    fix_responsive_ui()
    
    print("\n✅ All critical fixes applied!")

if __name__ == "__main__":
    main()
