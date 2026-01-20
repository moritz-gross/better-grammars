# Setting Up Python Environment with UV

This guide will help you set up a clean Python environment using `uv` for the Jupyter notebook analysis.

## Why UV?

- **10-100x faster** than pip/conda
- **Better dependency resolution** - no conflicts
- **Single binary** - no Python required to install
- **Works with pyproject.toml** - modern Python standard

## Step 1: Install UV

```bash
# Install uv (if not already installed)
curl -LsSf https://astral.sh/uv/install.sh | sh

# Or with homebrew
brew install uv
```

## Step 2: Create Virtual Environment

```bash
# Navigate to project directory
cd /Users/moritzgross/IdeaProjects/better-grammars-2

# Create a virtual environment with Python 3.10+
uv venv

# Activate the virtual environment
source .venv/bin/activate
```

## Step 3: Install Dependencies

```bash
# Install all dependencies from pyproject.toml
uv pip install -e .

# This installs:
# - pandas (data analysis)
# - matplotlib (plotting)
# - numpy (numerical operations)
# - jupyter (notebook interface)
# - notebook (Jupyter Notebook server)
# - nbconvert (export functionality)
# - ipykernel (Python kernel for notebooks)
```

## Step 4: Register Jupyter Kernel

```bash
# Register the virtual environment as a Jupyter kernel
python -m ipykernel install --user --name=better-grammars --display-name="Better Grammars (uv)"
```

## Step 5: Start Jupyter

```bash
# Start Jupyter Notebook (uses the new environment)
jupyter notebook

# Or start Jupyter Lab (modern interface)
jupyter lab
```

## Step 6: Open Your Notebook

1. Navigate to `visualize_local_search.ipynb`
2. Click "Kernel" → "Change Kernel" → "Better Grammars (uv)"
3. Run all cells - everything should work now!

## Step 7: Export to HTML (Should Work Now!)

### From Command Line:
```bash
# With the virtual environment activated:
jupyter nbconvert --to html visualize_local_search.ipynb --output results/notebook_export.html

# Or execute and export in one go:
jupyter nbconvert --to html --execute visualize_local_search.ipynb --output results/notebook_export.html
```

### From Jupyter Web UI:
- File → Download as → HTML (should work now without 500 errors)

## Running the Plot Generation Script

```bash
# Make sure virtual environment is activated
source .venv/bin/activate

# Run the plot generation
python generate_plots.py
```

## Adding New Dependencies

If you need additional packages:

```bash
# Add to pyproject.toml dependencies, then:
uv pip install -e .

# Or install directly:
uv pip install package-name
```

## Deactivating the Environment

```bash
deactivate
```

## Removing Old Jupyter Kernels (Cleanup)

If you have old broken kernels:

```bash
# List all kernels
jupyter kernelspec list

# Remove old/broken kernels
jupyter kernelspec remove kernel-name
```

## Troubleshooting

### Issue: "jupyter: command not found"
```bash
# Make sure virtual environment is activated
source .venv/bin/activate
```

### Issue: Kernel not showing up
```bash
# Re-register the kernel
python -m ipykernel install --user --name=better-grammars --display-name="Better Grammars (uv)"
```

### Issue: Export still fails
```bash
# Reinstall nbconvert
uv pip install --reinstall nbconvert
```

## Files Created

After setup, you'll have:
- `.venv/` - Virtual environment (git-ignored)
- `pyproject.toml` - Project dependencies
- All packages isolated in the virtual environment

## Daily Workflow

```bash
# 1. Navigate to project
cd /Users/moritzgross/IdeaProjects/better-grammars-2

# 2. Activate environment
source .venv/bin/activate

# 3. Start Jupyter
jupyter notebook

# 4. Work on notebooks...

# 5. When done
deactivate
```

## Benefits of This Setup

✅ **No system Python pollution** - everything isolated
✅ **Fast installs** - uv is blazingly fast
✅ **Reproducible** - exact same environment on any machine
✅ **No conflicts** - uv resolves dependencies properly
✅ **Works with Jupyter** - full notebook support
✅ **Export works** - nbconvert has proper templates
