package com.example.marketplace.service;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.CategoriaProduto;
import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    private BigDecimal calcularDescontoPorItem(ItemCarrinho item) {
        BigDecimal desconto = BigDecimal.ZERO;
        CategoriaProduto categoria = item.getProduto().getCategoria();
        switch(categoria)
        {
            case CAPINHA:
                desconto = BigDecimal.valueOf(0.03);
                break;
            case CARREGADOR:
                desconto = BigDecimal.valueOf(0.05);
                break;
            case FONE:
                desconto = BigDecimal.valueOf(0.03);
                break;
            case PELICULA:
                desconto = BigDecimal.valueOf(0.02);
                break;
            case SUPORTE:
                desconto = BigDecimal.valueOf(0.02);
                break;
            default:
                break;
        }
        return desconto;
    }

    private BigDecimal calcularDescontoPorQuantidade(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        int quantidadeTotal = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();
    
        if (quantidadeTotal == 2) {
            return BigDecimal.valueOf(0.05);
        } else if (quantidadeTotal == 3) {
            return BigDecimal.valueOf(0.07); 
        } else if (quantidadeTotal >= 4) {
            return BigDecimal.valueOf(0.10); 
        }

        return desconto;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // =======================================================
        // Calcula desconto considerando a categoria de cada item
        // ========================================================
        BigDecimal valorDescontoPorItem = itens.stream()
                .map(item -> item.calcularSubtotal()
                        .multiply(calcularDescontoPorItem(item)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // =====================================================
        // Calcula desconto considerando a quantidade de itens
        // ======================================================
        BigDecimal valorDescontoQuantidade = calcularDescontoPorQuantidade(itens).multiply(subtotal);

        // =================================================================
        // Calcula desconto = valorDescontoPorItem + valorDescontoQuantidade
        // =================================================================
        BigDecimal valorDescontoParcial = valorDescontoPorItem.add(valorDescontoQuantidade);

        // =======================================
        // Calcula percentual de desconto aplicado
        // =======================================
        BigDecimal percentualDesconto = valorDescontoParcial.divide(subtotal);
        if(percentualDesconto.compareTo(BigDecimal.valueOf(0.25)) > 0) {
            percentualDesconto = BigDecimal.valueOf(0.25);
        }

        BigDecimal valorDesconto = percentualDesconto.multiply(subtotal);

        // =======================
        // Calcula total
        // ========================
        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}
