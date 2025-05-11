import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import pandas as pd
from sklearn.preprocessing import KBinsDiscretizer
from typing import Tuple, List


def distribucion_proporcion_categorica(df: pd.DataFrame, col: str, target: str = 'y', target_value: int or str = 1, legend=False) -> plt.Figure:  # type: ignore
    fig, axs = plt.subplots(nrows=2, ncols=1, figsize=(12, 6))

    ax = sns.countplot(df, x=col, hue=col, ax=axs[0], legend=legend)

    for p in ax.patches:
        h = p.get_height()
        per = (h / df.shape[0]) * 100
        ax.annotate(
            f'{h:.0f} ({per:.1f}%)',
            (p.get_x() + p.get_width() / 2., h),
            ha='center', va='center',
            fontsize=9, color='black',
            xytext=(0, 5), textcoords='offset points'
        )
    ax.set_title(f'Distribución de "{col}"')
    ax.set_xlabel(col)
    ax.set_ylabel("Recuento")

    counts = df[col].value_counts(normalize=True)
    std_devs = np.sqrt(counts * (1 - counts) / df.shape[0])

    ax = sns.countplot(df[df[target] == target_value], x=col,
                       hue=col, ax=axs[1], stat='proportion', legend=legend)
    for p, count, std_dev in zip(ax.patches, counts, std_devs):
        h = p.get_height()
        x_pos = p.get_x() + p.get_width() / 2
        ax.annotate(
            f'{h:.2f}',
            (x_pos, h),
            ha='center', va='center',
            fontsize=9, color='black',
            xytext=(0, 5), textcoords='offset points'
        )
        ax.errorbar(x_pos, h, yerr=std_dev, fmt='none',
                    color='black', capsize=5)

    ax.set_title(f'Proporción de "{col}" respecto de valores "YES"')
    ax.set_xlabel(col)
    ax.set_ylabel("Proporción")
    plt.tight_layout()
    return fig


# type: ignore
def distribucion_proporcion_numerica(df: pd.DataFrame, col: str, len_discretizadores: Tuple[int], target: str = 'y', target_value: int or str = 1, legend=False) -> plt.Figure:
    # Métricas estadísticas
    stats = df[col].describe()
    q1 = df[col].quantile(0.25)
    q2 = df[col].quantile(0.50)
    q3 = df[col].quantile(0.75)
    mean = df[col].mean()

    # Crear discretizadores
    # Discretizador para el histograma y boxplot
    d1 = KBinsDiscretizer(
        n_bins=len_discretizadores[0], encode='ordinal', strategy='uniform')
    # Discretizador para el countplot
    d2 = KBinsDiscretizer(
        n_bins=len_discretizadores[1], encode='ordinal', strategy='uniform')

    # Discretizar la columna en múltiples intervalos
    df[f'{col}_bin'] = d1.fit_transform(df[[col]])

    # Crear figura con 3 subplots
    fig = plt.figure(figsize=(12, 11))
    gs = fig.add_gridspec(3, 1, height_ratios=[3, 1, 2])
    ax1 = fig.add_subplot(gs[0])
    ax2 = fig.add_subplot(gs[1], sharex=ax1)
    ax3 = fig.add_subplot(gs[2])

    # Histograma
    sns.histplot(df[col], kde=True, stat='density', ax=ax1,
                 color='skyblue', edgecolor='black')
    for line in ax1.lines:
        line.set_color("yellow")
    ax1.axvline(mean, color='red', linestyle='--', label=f"Media: {mean:.2f}")

    legend_text = (f"Min: {stats['min']:.2f}\nQ1: {q1:.2f}\nQ2: {q2:.2f}\n"
                   f"Q3: {q3:.2f}\nMax: {stats['max']:.2f}\nMedia: {mean:.2f}")
    ax1.legend(title=legend_text, loc='upper right')
    ax1.set_ylabel("Densidad")
    ax1.set_title("Distribución de edades")

    # 🔹 Boxplot
    sns.boxplot(x=col, data=df, ax=ax2, color='skyblue', orient='h')
    ax2.set_yticks([])

    # Countplot con proporciones y barras de error
    df[f'{col}_bin'] = d2.fit_transform(df[[col]]).astype(int)
    counts = df[f'{col}_bin'].value_counts(normalize=True)
    std_devs = np.sqrt(counts * (1 - counts) / df.shape[0])

    ax = sns.countplot(df[df[target] == target_value], x=f'{col}_bin', hue=target,
                       ax=ax3, stat='proportion', legend=legend, palette={1: 'skyblue'})
    for p, count, std_dev in zip(ax.patches, counts, std_devs):
        h = p.get_height()
        x_pos = p.get_x() + p.get_width() / 2
        ax.annotate(
            f'{h:.2f}',
            (x_pos, h),
            ha='center', va='center',
            fontsize=9, color='black',
            xytext=(0, 5), textcoords='offset points'
        )
        ax.errorbar(x_pos, h, yerr=std_dev, fmt='none',
                    color='black', capsize=5)
        p.set_edgecolor('black')  # Cambiar el color del borde de las barras

    # Los ejes deben ser del discretizador
    bin_edges = d2.bin_edges_[0]
    bin_labels = [
        f"{int(bin_edges[i])} - {int(bin_edges[i+1])}" for i in range(len(bin_edges)-1)]
    ax.set_xticks(range(len(bin_labels)))
    ax.set_xticklabels(bin_labels, rotation=45)
    ax.set_ylabel("Proporción de 'yes'")
    ax.set_xlabel(col)
    ax.set_title(f'Proporción de "{col}" respecto de valores "YES"')
    plt.xticks(rotation=45)
    plt.tight_layout()
    return fig


def mediana_por_mes(df: pd.DataFrame, target: str, col: str = 'month', legend=False, ordered_months: List[str] = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec']) -> plt.Figure:
    # median age by month
    median_age_by_month = df.groupby(col)[target].median().reset_index()
    median_age_by_month[col] = pd.Categorical(
        median_age_by_month[col], categories=ordered_months, ordered=True)
    median_age_by_month = median_age_by_month.sort_values(col)
    # Plotting
    fig = plt.figure(figsize=(12, 6))
    ax = sns.lineplot(data=median_age_by_month, x=col, y=target,
                      marker='o', color='skyblue', legend=legend)
    for i, row in median_age_by_month.iterrows():
        ax.text(row[col], row[target] + 0.1,
                f"{row[target]:.1f}", color='black', ha='center')
    plt.title(f'Mediana "{col}" por "{target}"')
    plt.xlabel(target)
    plt.ylabel(f'Mediana {col}')
    plt.xticks(rotation=45)
    return fig


def _redondear_en_mapa_de_calor(df, ax, highlight: str or tuple = '', circle_color: str = 'red'):  # type: ignore
    if highlight == 'max':
        val = df.max().max()
        coords = df.stack()[lambda x: x == val].index[0]
    elif highlight == 'min':
        val = df[df > 0].min().min()
        coords = df.stack()[lambda x: x == val].index[0]
    elif isinstance(highlight, tuple):
        coords = highlight
    else:
        raise ValueError(
            "highlight debe ser 'min', 'max' o una tupla (fila, columna)")

    # Convertir a coordenadas del heatmap
    row_idx = df.index.get_loc(coords[0])
    col_idx = df.columns.get_loc(coords[1])

    # Dibujar círculo
    ax.add_patch(plt.Circle((col_idx + 0.5, row_idx + 0.5), 0.25,
                            fill=False, edgecolor=circle_color, linewidth=2))

    # Cambiar color del texto del eje X e Y
    for label in ax.get_xticklabels():
        if label.get_text() == str(coords[1]):
            label.set_color(circle_color)

    for label in ax.get_yticklabels():
        if label.get_text() == str(coords[0]):
            label.set_color(circle_color)


# type: ignore
def mapas_de_calor_por_mes(df: pd.DataFrame, col: str, target: str, target_value: str or int = 1, month_col: str = "month", highlight: str or tuple = None, ordered_months: List[str] = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec']) -> plt.Figure:
    # Matriz de correlaciones con crosstab
    count_pivot = pd.crosstab(df[col], df[month_col])
    count_pivot = count_pivot.reindex(columns=ordered_months, fill_value=0)

    # Agrupamos por job + month y calculamos proporción de 'yes'
    grouped = df.groupby([col, month_col])[target].apply(
        lambda x: (x == target_value).sum() / len(x)).unstack(fill_value=0)
    grouped = grouped.reindex(columns=ordered_months, fill_value=0)

    # 🔲 Crear los dos heatmaps uno al lado del otro
    fig, axes = plt.subplots(1, 2, figsize=(18, 7), sharey=True)

    # Mapa 1: Recuento
    sns.heatmap(count_pivot, annot=True, fmt='d',
                cmap='Blues', linewidths=0.5, ax=axes[0])
    axes[0].set_title(f'Recuento por "{col}" y mes')
    axes[0].set_xlabel('Mes')
    axes[0].set_ylabel(f'{col}')
    axes[0].tick_params(axis='x', rotation=45)

    if highlight is not None:
        _redondear_en_mapa_de_calor(
            count_pivot, axes[0], highlight, circle_color='red')

    # Mapa 2: Proporción de 'yes'
    sns.heatmap(grouped, annot=True, fmt='.2f',
                cmap='Blues', linewidths=0.5, ax=axes[1])
    axes[1].set_title(f"Proporción de 'yes' por '{col}' y mes")
    axes[1].set_xlabel('Mes')
    axes[1].set_ylabel('')  # Ya lo muestra el primer subplot
    axes[1].tick_params(axis='x', rotation=45)

    if highlight is not None:
        _redondear_en_mapa_de_calor(
            count_pivot, axes[1], highlight, circle_color='red')

    plt.tight_layout()
    return fig
